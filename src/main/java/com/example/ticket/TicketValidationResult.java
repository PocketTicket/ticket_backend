package com.example.ticket;

/**
 * The verdict of {@link TicketValidator}. {@code reason} and {@code message} are
 * null exactly when {@code valid} is true.
 */
public record TicketValidationResult(
        boolean valid,
        TicketRejectionReason reason,
        String message
) {
    private static final TicketValidationResult ACCEPTED =
            new TicketValidationResult(true, null, null);

    public static TicketValidationResult accepted() {
        return ACCEPTED;
    }

    public static TicketValidationResult rejected(TicketRejectionReason reason, String message) {
        return new TicketValidationResult(false, reason, message);
    }
}
