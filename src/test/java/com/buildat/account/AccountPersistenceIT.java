package com.buildat.account;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import com.buildat.PostgresTestConfiguration;
import com.buildat.auth.CurrentIdentity.Identity;
import com.buildat.http.ApiFailure;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class AccountPersistenceIT {
    @Autowired AccountTransactions transactions;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManager entities;
    @Autowired PostgreSQLContainer postgres;
    @Autowired Flyway flyway;
    private Identity identity() { return new Identity("https://test.invalid/auth/v1", UUID.randomUUID(), "synthetic"); }

    @Test void concurrentBootstrapCreatesExactlyOneDurableMappingAndRepeatDoesNotTouchTimestamps() throws Exception {
        var identity = identity();
        try (var executor = Executors.newFixedThreadPool(8)) {
            var start = new CountDownLatch(1);
            var futures = new ArrayList<Future<AccountService.BootstrapResult>>();
            for (int i = 0; i < 8; i++) futures.add(executor.submit(() -> { start.await(); return transactions.createOrFind(identity); }));
            start.countDown();
            var results = new ArrayList<AccountService.BootstrapResult>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            assertThat(results.stream().filter(AccountService.BootstrapResult::created)).hasSize(1);
            assertThat(results.stream().map(result -> result.account().id()).distinct()).hasSize(1);
        }
        var template = new TransactionTemplate(manager);
        var before = template.execute(status -> entities.createNativeQuery("select updated_at from buildat.accounts where identity_subject = ?1")
                .setParameter(1, identity.subject()).getSingleResult());
        assertThat(transactions.createOrFind(identity).created()).isFalse();
        var after = template.execute(status -> entities.createNativeQuery("select updated_at from buildat.accounts where identity_subject = ?1")
                .setParameter(1, identity.subject()).getSingleResult());
        assertThat(after).isEqualTo(before);
    }

    @Test void restrictionsAreFreshAndTombstonesCannotBeRecreated() {
        var identity = identity(); var created = transactions.createOrFind(identity);
        var template = new TransactionTemplate(manager);
        template.executeWithoutResult(status -> entities.find(Account.class, created.account().id(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                .restrict(Account.Standing.SUSPENDED, Instant.now()));
        assertRestricted(() -> transactions.findActive(identity));
        assertRestricted(() -> transactions.createOrFind(identity));
        template.executeWithoutResult(status -> entities.find(Account.class, created.account().id(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
                .restrict(Account.Standing.DELETED, Instant.now()));
        assertRestricted(() -> transactions.createOrFind(identity));
        template.executeWithoutResult(status -> {
            var account = entities.find(Account.class, created.account().id());
            assertThatThrownBy(() -> account.restrict(Account.Standing.ACTIVE, Instant.now())).isInstanceOf(IllegalStateException.class);
        });
    }

    @Test void transactionRollbackLeavesNoMappingAndUniqueConstraintRejectsDuplicates() {
        var identity = identity();
        var template = new TransactionTemplate(manager);
        template.executeWithoutResult(status -> { transactions.createOrFind(identity); status.setRollbackOnly(); });
        assertThat(transactions.findActive(identity)).isEmpty();
        transactions.createOrFind(identity);
        assertThatThrownBy(() -> template.executeWithoutResult(status -> entities.createNativeQuery("""
                insert into buildat.accounts values (?1, ?2, ?3, 'ACTIVE', now(), now())
                """).setParameter(1, UUID.randomUUID()).setParameter(2, identity.issuer()).setParameter(3, identity.subject()).executeUpdate()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test void restrictedRuntimeCanReadAndCreateButCannotDeleteUpdateOrUseDdlAndBrowserCannotRead() throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), "buildat_test_app", "test-only");
                var statement = connection.createStatement()) {
            statement.executeQuery("select count(*) from buildat.accounts").close();
            statement.execute("insert into buildat.accounts values ('" + UUID.randomUUID() + "', 'test', '" + UUID.randomUUID() + "', 'ACTIVE', now(), now())");
            for (String sql : List.of("delete from buildat.accounts", "update buildat.accounts set standing='DELETED'", "create table buildat.unwanted(id int)",
                    "insert into buildat.accounts values ('" + UUID.randomUUID() + "', 'test', '" + UUID.randomUUID() + "', 'DELETED', now(), now())")) {
                assertThatThrownBy(() -> statement.execute(sql)).isInstanceOf(java.sql.SQLException.class);
            }
        }
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            for (String role : List.of("anon", "authenticated")) {
                statement.execute("set role " + role);
                assertThatThrownBy(() -> statement.executeQuery("select * from buildat.accounts")).isInstanceOf(java.sql.SQLException.class);
                statement.execute("reset role");
            }
        }
    }

    @Test void migrationUpgradesV1AndFailureCannotBeSilentlyBypassed() throws Exception {
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("create database phase2_upgrade"); statement.execute("create database phase2_failure");
        }
        String base = postgres.getJdbcUrl().substring(0, postgres.getJdbcUrl().lastIndexOf('/') + 1);
        var old = Flyway.configure().dataSource(base + "phase2_upgrade", postgres.getUsername(), postgres.getPassword()).target("1").load();
        old.migrate(); assertThat(old.info().applied()).hasSize(1);
        var upgrade = Flyway.configure().dataSource(base + "phase2_upgrade", postgres.getUsername(), postgres.getPassword()).load();
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(upgrade.validateWithResult().validationSuccessful).isTrue();
        var failed = Flyway.configure().dataSource(base + "phase2_failure", postgres.getUsername(), postgres.getPassword())
                .locations("classpath:failure-migrations").load();
        assertThatThrownBy(failed::migrate).isInstanceOf(org.flywaydb.core.api.FlywayException.class);
        assertThatThrownBy(failed::migrate).isInstanceOf(org.flywaydb.core.api.FlywayException.class);
    }
    private void assertRestricted(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ApiFailure.class, failure -> assertThat(failure.code()).isEqualTo("ACCOUNT_RESTRICTED"));
    }
}
