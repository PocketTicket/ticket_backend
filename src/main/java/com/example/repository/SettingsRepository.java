package com.example.repository;

import com.example.models.settings.Settings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import static com.example.jooq.generated.Tables.SETTINGS;

@ApplicationScoped
public class SettingsRepository {
    @Inject
    DSLContext jooq;

    public Settings getSettings() {
        return jooq.selectFrom(SETTINGS)
                .fetchSingle(record -> new Settings(
                        record.get(SETTINGS.SETTING_PAYMENT_DAYS),
                        record.get(SETTINGS.SETTING_ENTRY_MINUTES_BEFORE_START)));
    }

    public void updateSettings(Settings settings) {
        // No WHERE: the settings table has exactly one row.
        jooq.update(SETTINGS)
                .set(SETTINGS.SETTING_PAYMENT_DAYS, settings.paymentDays())
                .set(SETTINGS.SETTING_ENTRY_MINUTES_BEFORE_START, settings.entryMinutesBeforeStart())
                .execute();
    }
}
