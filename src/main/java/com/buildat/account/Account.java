package com.buildat.account;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "accounts", schema = "buildat")
class Account {
    enum Standing { ACTIVE, SUSPENDED, DELETED }
    @Id private UUID id;
    @Column(name = "identity_issuer", nullable = false, updatable = false) private String issuer;
    @Column(name = "identity_subject", nullable = false, updatable = false) private UUID subject;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text") private Standing standing;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Account() { }
    UUID id() { return id; }
    boolean active() { return standing == Standing.ACTIVE; }

    // Called only by future account-module restriction use cases under a row lock.
    void restrict(Standing target, Instant now) {
        java.util.Objects.requireNonNull(target);
        if (target == Standing.ACTIVE || (standing == Standing.DELETED && target != Standing.DELETED)) {
            throw new IllegalStateException("Account reactivation is not supported");
        }
        if (standing != target) {
            if (now.isBefore(updatedAt)) throw new IllegalArgumentException("Invalid account timestamp");
            standing = target;
            updatedAt = now;
        }
    }
}
