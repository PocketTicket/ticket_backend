package com.example.repository;

import com.example.models.admin.Admin;
import com.example.models.admin.AdminRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Record;

import static com.example.jooq.generated.Tables.ADMINS;

@ApplicationScoped
public class AdminRepository {
    @Inject
    DSLContext jooq;

    /** @return the account, or null if no account has that username. */
    public Admin getAdminByUsername(String username) {
        return jooq.selectFrom(ADMINS)
                .where(ADMINS.ADMIN_USERNAME.eq(username))
                .fetchOne(AdminRepository::toAdmin);
    }

    public boolean hasAccountWithRole(AdminRole role) {
        return jooq.fetchExists(ADMINS, ADMINS.ADMIN_ROLE.eq(role.name()));
    }

    /** Creates an account without email address. */
    public void createAdmin(String username, String passwordHash, AdminRole role) {
        jooq.insertInto(ADMINS)
                .set(ADMINS.ADMIN_USERNAME, username)
                .set(ADMINS.ADMIN_PASSWORD_HASH, passwordHash)
                .set(ADMINS.ADMIN_ROLE, role.name())
                .execute();
    }

    public void updateAccount(String username, String passwordHash, String email) {
        jooq.update(ADMINS)
                .set(ADMINS.ADMIN_PASSWORD_HASH, passwordHash)
                .set(ADMINS.ADMIN_EMAIL, email)
                .where(ADMINS.ADMIN_USERNAME.eq(username))
                .execute();
    }

    public void updateDoorStaffPassword(String passwordHash) {
        jooq.update(ADMINS)
                .set(ADMINS.ADMIN_PASSWORD_HASH, passwordHash)
                .where(ADMINS.ADMIN_ROLE.eq(AdminRole.DOOR_STAFF.name()))
                .execute();
    }

    private static Admin toAdmin(Record record) {
        return new Admin(
                record.get(ADMINS.ADMIN_USERNAME),
                AdminRole.valueOf(record.get(ADMINS.ADMIN_ROLE)),
                record.get(ADMINS.ADMIN_PASSWORD_HASH),
                record.get(ADMINS.ADMIN_EMAIL)
        );
    }
}
