package com.example.repository;

import com.example.models.user.AuthProvider;
import com.example.models.user.User;
import com.example.models.user.UserRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;

import static com.example.jooq.generated.Tables.USERS;
import static org.jooq.impl.DSL.lower;

@ApplicationScoped
public class UserRepository {
    @Inject
    DSLContext jooq;

    public List<User> getUsers() {
        return jooq.select(USERS.fields())
                .from(USERS)
                .orderBy(USERS.USER_ID)
                .fetch(UserRepository::toUser);
    }

    /** @return the user, or null if no user has that id. */
    public User getUserById(int userId) {
        return jooq.select(USERS.fields())
                .from(USERS)
                .where(USERS.USER_ID.eq(userId))
                .fetchOne(UserRepository::toUser);
    }

    /** Matched case-insensitively, the same way idx_users_email enforces uniqueness. */
    public User getUserByEmail(String email) {
        return jooq.select(USERS.fields())
                .from(USERS)
                .where(lower(USERS.USER_EMAIL).eq(email.toLowerCase()))
                .fetchOne(UserRepository::toUser);
    }

    public boolean existsById(int userId) {
        return jooq.fetchExists(jooq.selectOne().from(USERS).where(USERS.USER_ID.eq(userId)));
    }

    /** The userId and createdAt of the given model are ignored; the database assigns them. */
    public User createUser(User user) {
        return jooq.insertInto(USERS)
                .set(USERS.USER_FIRST_NAME, user.firstName())
                .set(USERS.USER_LAST_NAME, user.lastName())
                .set(USERS.USER_EMAIL, user.email())
                .set(USERS.USER_ROLE, toJooqRole(user.role()))
                .set(USERS.USER_AUTH_PROVIDER, toJooqProvider(user.authProvider()))
                .set(USERS.USER_EXTERNAL_ID, user.externalId())
                .set(USERS.USER_PASSWORD_HASH, user.passwordHash())
                .returning()
                .fetchOne(UserRepository::toUser);
    }

    private static User toUser(Record record) {
        return new User(
                record.get(USERS.USER_ID),
                record.get(USERS.USER_FIRST_NAME),
                record.get(USERS.USER_LAST_NAME),
                record.get(USERS.USER_EMAIL),
                UserRole.valueOf(record.get(USERS.USER_ROLE).getLiteral()),
                AuthProvider.valueOf(record.get(USERS.USER_AUTH_PROVIDER).getLiteral()),
                record.get(USERS.USER_EXTERNAL_ID),
                record.get(USERS.USER_PASSWORD_HASH),
                record.get(USERS.USER_CREATED_AT)
        );
    }

    private static com.example.jooq.generated.enums.UserRole toJooqRole(UserRole role) {
        return com.example.jooq.generated.enums.UserRole.valueOf(role.name());
    }

    private static com.example.jooq.generated.enums.AuthProvider toJooqProvider(AuthProvider provider) {
        return com.example.jooq.generated.enums.AuthProvider.valueOf(provider.name());
    }
}
