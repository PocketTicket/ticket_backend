package com.example.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SettingsRequest(
        @Min(value = 1, message = "paymentDays must be at least 1")
        @Max(value = 365, message = "paymentDays must not exceed 365")
        int paymentDays,

        @Min(value = 0, message = "entryMinutesBeforeStart must not be negative")
        @Max(value = 1440, message = "entryMinutesBeforeStart must not exceed 1440 (one day)")
        int entryMinutesBeforeStart
) { }
