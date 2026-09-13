package com.example.repository;

import com.example.models.order.OrderStatus;
import com.example.models.ticket.TicketEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import java.time.LocalDateTime;

import static com.example.jooq.generated.Tables.ORDERS;
import static com.example.jooq.generated.Tables.PRODUCTS;
import static com.example.jooq.generated.Tables.TICKETS;

@ApplicationScoped
public class TicketRepository {
    @Inject
    DSLContext jooq;

    /** @return the ticket with its order status and event, or null if the code belongs to no ticket. */
    public TicketEntry getTicketEntry(String code) {
        return jooq.select(
                        TICKETS.TICKET_CODE,
                        ORDERS.ORDER_STATUS,
                        TICKETS.TICKET_USED_AT,
                        PRODUCTS.PRODUCT_NAME,
                        PRODUCTS.PRODUCT_LOCATION,
                        PRODUCTS.PRODUCT_STARTS_AT)
                .from(TICKETS)
                .join(ORDERS).on(ORDERS.ORDER_ID.eq(TICKETS.TICKET_ORDER_ID))
                .join(PRODUCTS).on(PRODUCTS.PRODUCT_ID.eq(TICKETS.TICKET_PRODUCT_ID))
                .where(TICKETS.TICKET_CODE.eq(code))
                .fetchOne(record -> new TicketEntry(
                        record.value1(),
                        OrderStatus.PAID.name().equals(record.value2()),
                        record.value3(),
                        record.value4(),
                        record.value5(),
                        record.value6()));
    }

    /**
     * Uses the ticket up. The "still unused" check is part of the UPDATE, so two scanners
     * at the same moment cannot both let a guest in with the same ticket.
     *
     * @return false if the ticket was already used.
     */
    public boolean markTicketAsUsed(String code, LocalDateTime usedAt) {
        return jooq.update(TICKETS)
                .set(TICKETS.TICKET_USED_AT, usedAt)
                .where(TICKETS.TICKET_CODE.eq(code))
                .and(TICKETS.TICKET_USED_AT.isNull())
                .execute() > 0;
    }
}
