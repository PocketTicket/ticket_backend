package com.example.dto.settings;

public record SettingsResponse(
        int paymentDays,
        int entryMinutesBeforeStart
) { }
