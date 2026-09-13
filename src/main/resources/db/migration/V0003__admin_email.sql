-- The address people can contact the admin at. NULL until the admin finished the setup
-- after the first login with the default password (new password and email address).
ALTER TABLE admins
    ADD COLUMN admin_email VARCHAR(255);
