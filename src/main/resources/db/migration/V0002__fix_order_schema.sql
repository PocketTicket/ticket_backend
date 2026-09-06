-- Typo in the initial migration: the status is "paid", not "payed".
ALTER TYPE order_status RENAME VALUE 'PAYED' TO 'PAID';

-- V0001 indexed order_item_id, which is already covered by the primary key.
-- The lookup that actually needs an index is "all items of one order".
DROP INDEX IF EXISTS idx_order_items_order_id;
CREATE INDEX idx_order_items_order_id ON order_items (order_item_order_id);

-- Order items have no life of their own: deleting an order must take them with it.
-- Products stay protected by the default RESTRICT, so a product that was ever
-- ordered cannot silently disappear from someone's order history.
ALTER TABLE order_items
    DROP CONSTRAINT order_items_order_item_order_id_fkey;
ALTER TABLE order_items
    ADD CONSTRAINT order_items_order_item_order_id_fkey
        FOREIGN KEY (order_item_order_id) REFERENCES orders (order_id) ON DELETE CASCADE;
