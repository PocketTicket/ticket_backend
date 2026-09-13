package com.example.repository;

import com.example.models.order.Order;
import com.example.models.order.OrderStatus;
import com.example.models.ticket.Ticket;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.jooq.generated.Tables.ORDERS;
import static com.example.jooq.generated.Tables.PRODUCTS;
import static com.example.jooq.generated.Tables.TICKETS;
import static com.example.jooq.generated.Tables.USERS;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

@ApplicationScoped
public class OrderRepository {

    /** Loads the tickets of the surrounding ORDERS row in the same query. */
    private static final Field<List<Ticket>> TICKETS_OF_ORDER = multiset(
            select(TICKETS.TICKET_ID,
                    TICKETS.TICKET_CODE,
                    TICKETS.TICKET_PRODUCT_ID,
                    PRODUCTS.PRODUCT_NAME,
                    TICKETS.TICKET_PRICE)
                    .from(TICKETS)
                    .join(PRODUCTS).on(PRODUCTS.PRODUCT_ID.eq(TICKETS.TICKET_PRODUCT_ID))
                    .where(TICKETS.TICKET_ORDER_ID.eq(ORDERS.ORDER_ID))
                    .orderBy(TICKETS.TICKET_ID))
            .as("tickets")
            .convertFrom(result -> result.map(record -> new Ticket(
                    record.value1(),
                    record.value2(),
                    record.value3(),
                    record.value4(),
                    // The multiset travels as JSON, which turns 45.00 into 45.0.
                    record.value5().setScale(2))));

    @Inject
    DSLContext jooq;

    public List<Order> getOrders() {
        return selectOrders()
                .orderBy(ORDERS.ORDER_ID)
                .fetch(OrderRepository::toOrder);
    }

    /** @return the order, or null if no order has that id. */
    public Order getOrderById(int orderId) {
        return selectOrders()
                .where(ORDERS.ORDER_ID.eq(orderId))
                .fetchOne(OrderRepository::toOrder);
    }

    /**
     * Inserts the order and all of its tickets. The ids and createdAt of the given
     * models are ignored; the database assigns them.
     *
     * @return the stored order, re-read so it carries the generated values.
     */
    public Order createOrder(Order order) {
        int orderId = jooq.insertInto(ORDERS)
                .set(ORDERS.ORDER_USER_ID, order.user().userId())
                .set(ORDERS.ORDER_STATUS, order.status().name())
                .set(ORDERS.ORDER_PAYMENT_DUE_AT, order.paymentDueAt())
                .returning(ORDERS.ORDER_ID)
                .fetchOne(ORDERS.ORDER_ID);

        var insert = jooq.insertInto(TICKETS,
                TICKETS.TICKET_ORDER_ID,
                TICKETS.TICKET_PRODUCT_ID,
                TICKETS.TICKET_CODE,
                TICKETS.TICKET_PRICE);

        for (Ticket ticket : order.tickets()) {
            insert = insert.values(orderId, ticket.productId(), ticket.code(), ticket.price());
        }
        insert.execute();

        return getOrderById(orderId);
    }

    /**
     * Marks a pending order as paid. The status check is part of the UPDATE, so
     * two admins clicking at the same moment cannot send the tickets twice.
     *
     * @return false if the order was not pending.
     */
    public boolean markOrderAsPaid(int orderId, LocalDateTime paidAt) {
        return jooq.update(ORDERS)
                .set(ORDERS.ORDER_STATUS, OrderStatus.PAID.name())
                .set(ORDERS.ORDER_PAID_AT, paidAt)
                .where(ORDERS.ORDER_ID.eq(orderId))
                .and(ORDERS.ORDER_STATUS.eq(OrderStatus.PENDING.name()))
                .execute() > 0;
    }

    private SelectJoinStep<? extends Record> selectOrders() {
        return jooq.select(
                        ORDERS.ORDER_ID,
                        ORDERS.ORDER_STATUS,
                        ORDERS.ORDER_CREATED_AT,
                        ORDERS.ORDER_PAYMENT_DUE_AT,
                        ORDERS.ORDER_PAID_AT,
                        USERS.USER_ID,
                        USERS.USER_SSO_SUBJECT,
                        USERS.USER_EMAIL,
                        USERS.USER_FIRST_NAME,
                        USERS.USER_LAST_NAME,
                        TICKETS_OF_ORDER)
                .from(ORDERS)
                .join(USERS).on(USERS.USER_ID.eq(ORDERS.ORDER_USER_ID));
    }

    private static Order toOrder(Record record) {
        return new Order(
                record.get(ORDERS.ORDER_ID),
                UserRepository.toUser(record),
                OrderStatus.valueOf(record.get(ORDERS.ORDER_STATUS)),
                record.get(ORDERS.ORDER_CREATED_AT),
                record.get(ORDERS.ORDER_PAYMENT_DUE_AT),
                record.get(ORDERS.ORDER_PAID_AT),
                record.get(TICKETS_OF_ORDER)
        );
    }
}
