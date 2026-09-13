package com.example.service;

import com.example.dto.settings.SettingsRequest;
import com.example.dto.settings.SettingsResponse;
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
        return new SettingsResponse(settingsRepository.getPaymentDays());
    }

    /**
     * A new number of payment days applies to orders placed from now on. Existing orders
     * keep the due date their customers were already emailed.
     */
    public SettingsResponse updateSettings(SettingsRequest request) {
        settingsRepository.setPaymentDays(request.paymentDays());
        return getSettings();
    }
}
