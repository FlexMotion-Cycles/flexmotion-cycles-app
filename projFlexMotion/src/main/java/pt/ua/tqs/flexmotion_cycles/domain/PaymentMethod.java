package pt.ua.tqs.flexmotion_cycles.domain;

import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A card linked in the payment sandbox (US-14, BR2). Only the gateway token and the last four
 * digits are kept, never card data. A user may own several cards; one is the default.
 */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    private static final Pattern LAST_FOUR_DIGITS = Pattern.compile("\\d{4}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Authenticated username until the User entity backs the login (US-01), as in Trip. */
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String gatewayToken;

    @Column(name = "last4", nullable = false, length = 4)
    private String last4;

    @Column(name = "is_default", nullable = false)
    private boolean defaultMethod;

    protected PaymentMethod() {
        // Required by JPA.
    }

    /** A newly linked card; it is not the default until {@link #makeDefault()} is called. */
    public PaymentMethod(String username, String gatewayToken, String last4) {
        this.username = requireText(username, "username");
        this.gatewayToken = requireText(gatewayToken, "gatewayToken");
        Objects.requireNonNull(last4, "last4");
        if (!LAST_FOUR_DIGITS.matcher(last4).matches()) {
            throw new IllegalArgumentException("last4 must be exactly four digits");
        }
        this.last4 = last4;
        this.defaultMethod = false;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public void makeDefault() {
        this.defaultMethod = true;
    }

    public void clearDefault() {
        this.defaultMethod = false;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getGatewayToken() {
        return gatewayToken;
    }

    public String getLast4() {
        return last4;
    }

    public boolean isDefault() {
        return defaultMethod;
    }
}
