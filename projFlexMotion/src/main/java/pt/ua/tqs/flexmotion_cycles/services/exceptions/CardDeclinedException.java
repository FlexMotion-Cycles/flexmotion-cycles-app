package pt.ua.tqs.flexmotion_cycles.services.exceptions;

/** The payment sandbox declined the card, so it is not linked (BR2). */
public class CardDeclinedException extends RuntimeException {

    public CardDeclinedException() {
        super("The payment sandbox declined the card");
    }
}
