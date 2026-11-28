package pt.ua.tqs.flexmotion_cycles.ports;

/** Gateway answer to linking a card: the reusable token and last four digits when approved. */
public record CardLinkResult(boolean approved, String gatewayToken, String last4) {

    public static CardLinkResult approved(String gatewayToken, String last4) {
        return new CardLinkResult(true, gatewayToken, last4);
    }

    public static CardLinkResult declined() {
        return new CardLinkResult(false, null, null);
    }
}
