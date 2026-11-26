package pt.ua.tqs.flexmotion_cycles.domain;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A platform account with exactly one {@link Role} (US-02) and the data needed by the
 * registration and login rules (US-01, BR1–BR3). New accounts start as an active
 * {@link Role#END_USER} with an unverified e-mail and no failed logins.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Login identifier; also the principal name seen by the controllers. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Hash only; the plain password is never stored. */
    @Column(nullable = false)
    private String passwordHash;

    /** Used by the minimum age rule (BR1). */
    @Column(nullable = false)
    private LocalDate birthDate;

    /** An unverified account cannot reserve or start a trip (BR2). */
    @Column(nullable = false)
    private boolean emailVerified;

    /** Consecutive failed logins (BR3). */
    @Column(nullable = false)
    private int failedLogins;

    /** End of the login lock (BR3); null while the account is not locked. */
    private Instant lockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    /** Optimistic locking: concurrent changes on the same user fail. */
    @Version
    private Long version;

    protected User() {
        // Required by JPA.
    }

    /** A new account is an active END_USER with an unverified e-mail and no failed logins. */
    public User(String name, String email, String passwordHash, LocalDate birthDate) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.birthDate = birthDate;
        this.emailVerified = false;
        this.failedLogins = 0;
        this.role = Role.END_USER;
        this.status = AccountStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public int getFailedLogins() {
        return failedLogins;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }
}