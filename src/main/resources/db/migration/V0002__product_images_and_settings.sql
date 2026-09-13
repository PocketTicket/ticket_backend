-- The picture of a ticket type. Kept out of products, so listing products does not
-- load every picture.
CREATE TABLE product_images
(
    product_image_product_id   INT         PRIMARY KEY REFERENCES products (product_id),
    product_image_content_type VARCHAR(50) NOT NULL,
    product_image_data         BYTEA       NOT NULL
);

-- Settings the admin can change in the admin panel. The CHECK allows exactly one row.
CREATE TABLE settings
(
    setting_id           BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (setting_id),
    -- Days a customer has to pay before the reserved tickets are released
    setting_payment_days INT     NOT NULL CHECK (setting_payment_days > 0)
);
INSERT INTO settings (setting_payment_days) VALUES (7);
