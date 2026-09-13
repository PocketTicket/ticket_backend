package com.example.models.settings;

/** The settings the admin can change in the admin panel. */
public record Settings(
        // Days a customer has to pay before the reserved tickets are released
        int paymentDays,
        // Minutes before an event's start from which its tickets are accepted at the entrance
        int entryMinutesBeforeStart
) { }
