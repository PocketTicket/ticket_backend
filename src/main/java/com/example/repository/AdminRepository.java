package com.example.repository;

import com.example.models.admin.Admin;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Record;

import static com.example.jooq.generated.Tables.ADMINS;

@ApplicationScoped
public class AdminRepository {
    @Inject
    DSLContext jooq;

    /** @return the admin, or null if no admin has that username. */
    public Admin getAdminByUsername(String username) {
        return jooq.selectFrom(ADMINS)
                .where(ADMINS.ADMIN_USERNAME.eq(username))
                .fetchOne(AdminRepository::toAdmin);
    }

    public boolean hasAdmins() {
        return jooq.fetchExists(ADMINS);
    }

    /** Creates an admin without email address, i.e. one that still has to finish the setup. */
    public void createAdmin(String username, String passwordHash) {
        jooq.insertInto(ADMINS)
                .set(ADMINS.ADMIN_USERNAME, username)
                .set(ADMINS.ADMIN_PASSWORD_HASH, passwordHash)
                .execute();
    }

    public void updateAccount(String username, String passwordHash, String email) {
        jooq.update(ADMINS)
                .set(ADMINS.ADMIN_PASSWORD_HASH, passwordHash)
                .set(ADMINS.ADMIN_EMAIL, email)
                .where(ADMINS.ADMIN_USERNAME.eq(username))
                .execute();
    }

    private static Admin toAdmin(Record record) {
        return new Admin(
                record.get(ADMINS.ADMIN_ID),
                record.get(ADMINS.ADMIN_USERNAME),
                record.get(ADMINS.ADMIN_PASSWORD_HASH),
                record.get(ADMINS.ADMIN_EMAIL)
        );
    }
}
