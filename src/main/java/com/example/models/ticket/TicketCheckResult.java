package com.example.models.ticket;

/** The outcome of checking a ticket. The ticket page shows a message for each. */
public enum TicketCheckResult {
    /** Paid, unused and entry is open. Only answered when the ticket is not being used up. */
    VALID,
    /** Door check only: the ticket was valid and has just been used up, the guest may enter. */
    ADMITTED,
    /** Paid and unused, but entry for its event has not opened yet. */
    NOT_YET_OPEN,
    /** Somebody already entered with this ticket. */
    ALREADY_USED,
    /** Unknown code, or the order of the ticket is not paid (pending or cancelled). */
    INVALID
}
