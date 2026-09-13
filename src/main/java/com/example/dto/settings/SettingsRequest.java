package com.example.dto.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SettingsRequest(
        @Min(value = 1, message = "paymentDays must be at least 1")
        @Max(value = 365, message = "paymentDays must not exceed 365")
        int paymentDays
) { }
