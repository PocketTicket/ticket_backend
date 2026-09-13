package com.example.models.user;

/** A customer. Everything here comes from the SSO. */
public record User(
        int userId,
        // The id the SSO knows this person by.
        String ssoSubject,
        String email,
        String firstName,
        String lastName
) { }
