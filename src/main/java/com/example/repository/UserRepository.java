package com.example.repository;

import com.example.models.user.SsoProvider;
import com.example.models.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Record;

import static com.example.jooq.generated.Tables.USERS;

@ApplicationScoped
public class UserRepository {
    @Inject
    DSLContext jooq;

    /**
     * Inserts the user, or updates email and names if the provider already sent this
     * subject before. The userId of the given model is ignored.
     */
    public User saveUser(User user) {
        return jooq.insertInto(USERS)
                .set(USERS.USER_SSO_PROVIDER, user.ssoProvider().name())
                .set(USERS.USER_SSO_SUBJECT, user.ssoSubject())
                .set(USERS.USER_EMAIL, user.email())
                .set(USERS.USER_FIRST_NAME, user.firstName())
                .set(USERS.USER_LAST_NAME, user.lastName())
                .onConflict(USERS.USER_SSO_PROVIDER, USERS.USER_SSO_SUBJECT)
                .doUpdate()
                .set(USERS.USER_EMAIL, user.email())
                .set(USERS.USER_FIRST_NAME, user.firstName())
                .set(USERS.USER_LAST_NAME, user.lastName())
                .returning()
                .fetchOne(UserRepository::toUser);
    }

    /** Also used by OrderRepository and SessionRepository, which join the user in. */
    static User toUser(Record record) {
        return new User(
                record.get(USERS.USER_ID),
                SsoProvider.valueOf(record.get(USERS.USER_SSO_PROVIDER)),
                record.get(USERS.USER_SSO_SUBJECT),
                record.get(USERS.USER_EMAIL),
                record.get(USERS.USER_FIRST_NAME),
                record.get(USERS.USER_LAST_NAME)
        );
    }
}
