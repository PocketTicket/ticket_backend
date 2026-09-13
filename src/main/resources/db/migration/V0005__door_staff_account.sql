-- ADMIN uses the admin panel. DOOR_STAFF is the account all door staff share; it can only
-- check tickets in at the entrance.
ALTER TABLE admins
    ADD COLUMN admin_role VARCHAR(20) NOT NULL DEFAULT 'ADMIN'
        CHECK (admin_role IN ('ADMIN', 'DOOR_STAFF'));
