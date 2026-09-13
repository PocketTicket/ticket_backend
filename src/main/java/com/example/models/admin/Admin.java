package com.example.models.admin;

/** A login of the admin login form: an admin or the shared door staff account. Neither comes from the SSO. */
public record Admin(
        String username,
        AdminRole role,
        // bcrypt hash, never the password itself
        String passwordHash,
        // Where people can contact the admin. Null until the first-login setup is done,
        // and always null for the door staff account.
        String email
) {
    /** True for an admin who has not yet replaced the default password and entered an email address. */
    public boolean setupRequired() {
        return role == AdminRole.ADMIN && email == null;
    }
}
