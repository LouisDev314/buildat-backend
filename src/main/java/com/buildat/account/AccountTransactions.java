package com.buildat.account;

import java.util.Optional;
import java.util.UUID;
import com.buildat.auth.CurrentIdentity.Identity;
import com.buildat.http.ApiFailure;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountTransactions {
    private final AccountRepository repository;
    AccountTransactions(AccountRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Optional<AccountService.AccountView> findActive(Identity identity) {
        return repository.findIdentity(identity.issuer(), identity.subject()).map(AccountTransactions::active);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 5)
    public AccountService.BootstrapResult createOrFind(Identity identity) {
        boolean created = repository.insertIfAbsent(UUID.randomUUID(), identity.issuer(), identity.subject()) == 1;
        var account = repository.lockIdentity(identity.issuer(), identity.subject())
                .orElseThrow(() -> new ApiFailure(409, "ACCOUNT_STATE_CONFLICT"));
        return new AccountService.BootstrapResult(active(account), created);
    }

    private static AccountService.AccountView active(Account account) {
        if (!account.active()) throw new ApiFailure(403, "ACCOUNT_RESTRICTED");
        return new AccountService.AccountView(account.id());
    }
}
