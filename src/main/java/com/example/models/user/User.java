package com.example.models.user;

import java.time.LocalDateTime;

/**
 * A person who can order tickets. {@code externalId} is the subject given by
 * IServ or Moodle and is null for LOCAL accounts; {@code passwordHash} is the
 * other way round and is never handed out of the service layer.
 */
public record User(
        int userId,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        AuthProvider authProvider,
        String externalId,
        String passwordHash,
        LocalDateTime createdAt
) { }
