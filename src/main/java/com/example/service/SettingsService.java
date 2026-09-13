package com.example.service;

import com.example.dto.settings.SettingsRequest;
import com.example.dto.settings.SettingsResponse;
import com.example.mapper.SettingsMapper;
import com.example.repository.SettingsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class SettingsService {
    @Inject
    SettingsRepository settingsRepository;

    public SettingsResponse getSettings() {
        return SettingsMapper.toResponse(settingsRepository.getSettings());
    }

    /**
     * A new number of payment days applies to orders placed from now on. Existing orders
     * keep the due date their customers were already emailed. A new entry time applies
     * right away.
     */
    public SettingsResponse updateSettings(SettingsRequest request) {
        settingsRepository.updateSettings(SettingsMapper.toModel(request));
        return getSettings();
    }
}
