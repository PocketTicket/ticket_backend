-- Customers log in via IServ or Moodle. A subject is only unique within its provider.
ALTER TABLE users
    ADD COLUMN user_sso_provider VARCHAR(20) NOT NULL DEFAULT 'ISERV'
        CHECK (user_sso_provider IN ('ISERV', 'MOODLE'));
ALTER TABLE users
    ALTER COLUMN user_sso_provider DROP DEFAULT;
ALTER TABLE users
    DROP CONSTRAINT users_user_sso_subject_key;
ALTER TABLE users
    ADD CONSTRAINT users_sso_identity_key UNIQUE (user_sso_provider, user_sso_subject);

-- A logged-in customer. The browser keeps a random token in a cookie; only its SHA-256 hash
-- is stored, so the table alone cannot be used to take over a session.
CREATE TABLE user_sessions
(
    session_token_hash VARCHAR(64) PRIMARY KEY,
    session_user_id    INT       NOT NULL REFERENCES users (user_id),
    session_expires_at TIMESTAMP NOT NULL
);
