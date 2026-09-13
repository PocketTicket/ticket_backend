package com.example.repository;

import com.example.models.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static com.example.jooq.generated.Tables.USERS;
import static com.example.jooq.generated.Tables.USER_SESSIONS;

/**
 * The sessions of logged-in customers. Only the SHA-256 hash of a session token is stored,
 * so the table alone cannot be used to take over a session.
 */
@ApplicationScoped
public class SessionRepository {
    @Inject
    DSLContext jooq;

    public void createSession(String token, int userId, LocalDateTime expiresAt) {
        jooq.insertInto(USER_SESSIONS)
                .set(USER_SESSIONS.SESSION_TOKEN_HASH, hash(token))
                .set(USER_SESSIONS.SESSION_USER_ID, userId)
                .set(USER_SESSIONS.SESSION_EXPIRES_AT, expiresAt)
                .execute();
    }

    /** @return the user of the session, or null if the token is unknown or the session has expired. */
    public User getUserBySessionToken(String token, LocalDateTime now) {
        return jooq.select(USERS.fields())
                .from(USER_SESSIONS)
                .join(USERS).on(USERS.USER_ID.eq(USER_SESSIONS.SESSION_USER_ID))
                .where(USER_SESSIONS.SESSION_TOKEN_HASH.eq(hash(token)))
                .and(USER_SESSIONS.SESSION_EXPIRES_AT.gt(now))
                .fetchOne(UserRepository::toUser);
    }

    public void deleteSession(String token) {
        jooq.deleteFrom(USER_SESSIONS)
                .where(USER_SESSIONS.SESSION_TOKEN_HASH.eq(hash(token)))
                .execute();
    }

    public void deleteExpiredSessions(LocalDateTime now) {
        jooq.deleteFrom(USER_SESSIONS)
                .where(USER_SESSIONS.SESSION_EXPIRES_AT.le(now))
                .execute();
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java runtime has SHA-256", e);
        }
    }
}
