-- When the ticket was used at the entrance. NULL while it has not been used.
ALTER TABLE tickets
    ADD COLUMN ticket_used_at TIMESTAMP;

-- Minutes before an event's start from which its tickets are accepted at the entrance.
ALTER TABLE settings
    ADD COLUMN setting_entry_minutes_before_start INT NOT NULL DEFAULT 0
        CHECK (setting_entry_minutes_before_start >= 0);
