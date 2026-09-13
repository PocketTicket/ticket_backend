package com.example.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import static com.example.jooq.generated.Tables.SETTINGS;

@ApplicationScoped
public class SettingsRepository {
    @Inject
    DSLContext jooq;

    public int getPaymentDays() {
        return jooq.select(SETTINGS.SETTING_PAYMENT_DAYS)
                .from(SETTINGS)
                .fetchSingle(SETTINGS.SETTING_PAYMENT_DAYS);
    }

    public void setPaymentDays(int paymentDays) {
        // No WHERE: the settings table has exactly one row.
        jooq.update(SETTINGS)
                .set(SETTINGS.SETTING_PAYMENT_DAYS, paymentDays)
                .execute();
    }
}
