package com.example.mapper;

import com.example.dto.settings.SettingsRequest;
import com.example.dto.settings.SettingsResponse;
import com.example.models.settings.Settings;

public final class SettingsMapper {

    private SettingsMapper() {
    }

    public static Settings toModel(SettingsRequest request) {
        return new Settings(request.paymentDays(), request.entryMinutesBeforeStart());
    }

    public static SettingsResponse toResponse(Settings settings) {
        return new SettingsResponse(settings.paymentDays(), settings.entryMinutesBeforeStart());
    }
}
