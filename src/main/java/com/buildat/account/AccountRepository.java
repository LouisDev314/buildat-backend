package com.buildat.account;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface AccountRepository extends Repository<Account, UUID> {
    @Query("select a from Account a where a.issuer = :issuer and a.subject = :subject")
    Optional<Account> findIdentity(@Param("issuer") String issuer, @Param("subject") UUID subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.issuer = :issuer and a.subject = :subject")
    Optional<Account> lockIdentity(@Param("issuer") String issuer, @Param("subject") UUID subject);

    @Modifying
    @Query(value = """
            insert into buildat.accounts(id, identity_issuer, identity_subject, standing, created_at, updated_at)
            values (:id, :issuer, :subject, 'ACTIVE', current_timestamp, current_timestamp)
            on conflict on constraint accounts_external_identity_unique do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("issuer") String issuer, @Param("subject") UUID subject);
}
