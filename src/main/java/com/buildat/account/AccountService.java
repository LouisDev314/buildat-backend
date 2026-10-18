package com.buildat.account;

import java.util.UUID;
import com.buildat.auth.CurrentIdentity;
import com.buildat.auth.SupabaseUserVerifier;
import com.buildat.http.ApiFailure;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    public record AccountView(UUID id) { }
    public record BootstrapResult(AccountView account, boolean created) { }
    private final CurrentIdentity identities;
    private final SupabaseUserVerifier verifier;
    private final AccountTransactions transactions;
    AccountService(CurrentIdentity identities, SupabaseUserVerifier verifier, AccountTransactions transactions) {
        this.identities = identities;
        this.verifier = verifier;
        this.transactions = transactions;
    }
    public AccountView requireActiveAccount() {
        return transactions.findActive(identities.requirePermanentIdentity())
                .orElseThrow(() -> new ApiFailure(403, "ACCOUNT_BOOTSTRAP_REQUIRED"));
    }
    public BootstrapResult bootstrap() {
        var identity = identities.requirePermanentIdentity();
        var existing = transactions.findActive(identity);
        if (existing.isPresent()) return new BootstrapResult(existing.get(), false);
        // No database transaction or connection is held across the provider call.
        verifier.verify(identity);
        return transactions.createOrFind(identity);
    }
}
