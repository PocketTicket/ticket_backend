package com.example.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import static com.example.jooq.generated.Tables.ADMINS;

@ApplicationScoped
public class AdminRepository {
    @Inject
    DSLContext jooq;

    /** @return the bcrypt hash of the admin's password, or null if no admin has that username. */
    public String getPasswordHash(String username) {
        return jooq.select(ADMINS.ADMIN_PASSWORD_HASH)
                .from(ADMINS)
                .where(ADMINS.ADMIN_USERNAME.eq(username))
                .fetchOne(ADMINS.ADMIN_PASSWORD_HASH);
    }

    /** Creates the admin, or sets the new password if the username already exists. */
    public void saveAdmin(String username, String passwordHash) {
        jooq.insertInto(ADMINS)
                .set(ADMINS.ADMIN_USERNAME, username)
                .set(ADMINS.ADMIN_PASSWORD_HASH, passwordHash)
                .onConflict(ADMINS.ADMIN_USERNAME)
                .doUpdate()
                .set(ADMINS.ADMIN_PASSWORD_HASH, passwordHash)
                .execute();
    }
}
