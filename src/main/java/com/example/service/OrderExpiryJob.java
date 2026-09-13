package com.example.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cancels orders that were not paid in time. Each order is handled in its own
 * transaction, so a problem with one order does not undo the others.
 */
@ApplicationScoped
public class OrderExpiryJob {
    @Inject
    OrderService orderService;

    @Scheduled(every = "1m")
    void cancelExpiredOrders() {
        for (int orderId : orderService.getExpiredOrderIds()) {
            orderService.expireOrder(orderId);
        }
    }
}
