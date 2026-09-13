package com.example.models.user;

/** A customer. Everything here comes from IServ or Moodle. */
public record User(
        int userId,
        SsoProvider ssoProvider,
        // The id the provider knows this person by; only unique within that provider.
        String ssoSubject,
        String email,
        String firstName,
        String lastName
) { }
