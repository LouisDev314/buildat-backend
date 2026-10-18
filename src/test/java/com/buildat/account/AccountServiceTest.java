package com.buildat.account;

import java.util.*;
import com.buildat.auth.*;
import com.buildat.http.ApiFailure;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {
    private final CurrentIdentity identities = mock(CurrentIdentity.class);
    private final SupabaseUserVerifier verifier = mock(SupabaseUserVerifier.class);
    private final AccountTransactions transactions = mock(AccountTransactions.class);
    private final AccountService service = new AccountService(identities, verifier, transactions);
    private final CurrentIdentity.Identity identity = new CurrentIdentity.Identity("issuer", UUID.randomUUID(), "synthetic");

    @Test void existingMappingNeedsNoProviderCallAndRetrievalNeverCreates() {
        when(identities.requirePermanentIdentity()).thenReturn(identity);
        var account = new AccountService.AccountView(UUID.randomUUID());
        when(transactions.findActive(identity)).thenReturn(Optional.of(account));
        assertThat(service.requireActiveAccount()).isEqualTo(account);
        assertThat(service.bootstrap()).isEqualTo(new AccountService.BootstrapResult(account, false));
        verifyNoInteractions(verifier); verify(transactions, never()).createOrFind(any());
    }
    @Test void verifiesBeforeOpeningCreationTransactionAndFailureCreatesNothing() {
        when(identities.requirePermanentIdentity()).thenReturn(identity);
        when(transactions.findActive(identity)).thenReturn(Optional.empty());
        doThrow(new ApiFailure(503, "IDENTITY_PROVIDER_UNAVAILABLE")).when(verifier).verify(identity);
        assertThatThrownBy(service::bootstrap).isInstanceOf(ApiFailure.class);
        verify(transactions, never()).createOrFind(any());
        assertThatThrownBy(service::requireActiveAccount).isInstanceOfSatisfying(ApiFailure.class,
                failure -> assertThat(failure.code()).isEqualTo("ACCOUNT_BOOTSTRAP_REQUIRED"));
    }
    @Test void firstBootstrapVerifiesBeforeInsertAndRestrictedMappingsNeverReachProvider() {
        when(identities.requirePermanentIdentity()).thenReturn(identity);
        when(transactions.findActive(identity)).thenReturn(Optional.empty());
        service.bootstrap();
        var order = inOrder(transactions, verifier);
        order.verify(transactions).findActive(identity); order.verify(verifier).verify(identity); order.verify(transactions).createOrFind(identity);
        reset(verifier);
        when(transactions.findActive(identity)).thenThrow(new ApiFailure(403, "ACCOUNT_RESTRICTED"));
        assertThatThrownBy(service::bootstrap).isInstanceOf(ApiFailure.class);
        verifyNoInteractions(verifier);
    }
}
