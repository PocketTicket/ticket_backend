package com.example.models.admin;

/** An admin of the admin panel. Admins do not come from the SSO. */
public record Admin(
        int adminId,
        String username,
        // bcrypt hash, never the password itself
        String passwordHash,
        // Where people can contact the admin. Null until the first-login setup is done.
        String email
) {
    /** True until the admin replaced the default password and entered an email address. */
    public boolean setupRequired() {
        return email == null;
    }
}
