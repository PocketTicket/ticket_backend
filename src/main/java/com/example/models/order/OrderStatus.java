package com.example.models.order;

public enum OrderStatus {
    /** The tickets are reserved, the bank transfer has not arrived yet. */
    PENDING,
    PAID,
    /** Not paid in time; the tickets were released again. */
    CANCELLED
}
