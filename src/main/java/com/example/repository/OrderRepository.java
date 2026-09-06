package com.example.repository;

import com.example.jooq.generated.tables.records.OrdersRecord;
import com.example.models.order.Order;
import com.example.models.order.OrderItem;
import com.example.models.order.OrderStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStep4;
import org.jooq.Record;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.jooq.generated.Tables.ORDERS;
import static com.example.jooq.generated.Tables.ORDER_ITEMS;
import static com.example.jooq.generated.Tables.PRODUCTS;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

@ApplicationScoped
public class OrderRepository {

    /**
     * Correlated sub-select that loads the items of the surrounding ORDERS row in
     * the same round trip, so listing orders stays a single query. Typed, so the
     * result can be read back with record.get(ITEMS) without an unchecked cast.
     */
    private static final Field<List<OrderItem>> ITEMS = multiset(
            select(ORDER_ITEMS.ORDER_ITEM_ID,
                    ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID,
                    PRODUCTS.PRODUCT_NAME,
                    ORDER_ITEMS.ORDER_ITEM_QUANTITY,
                    ORDER_ITEMS.ORDER_ITEM_PRICE)
                    .from(ORDER_ITEMS)
                    .join(PRODUCTS).on(PRODUCTS.PRODUCT_ID.eq(ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID))
                    .where(ORDER_ITEMS.ORDER_ITEM_ORDER_ID.eq(ORDERS.ORDER_ID))
                    .orderBy(ORDER_ITEMS.ORDER_ITEM_ID))
            .as("items")
            .convertFrom(result -> result.map(record -> new OrderItem(
                    record.get(ORDER_ITEMS.ORDER_ITEM_ID),
                    record.get(ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID),
                    record.get(PRODUCTS.PRODUCT_NAME),
                    record.get(ORDER_ITEMS.ORDER_ITEM_QUANTITY),
                    record.get(ORDER_ITEMS.ORDER_ITEM_PRICE))));

    @Inject
    DSLContext jooq;

    public List<Order> getOrders() {
        return selectOrders()
                .orderBy(ORDERS.ORDER_ID)
                .fetch(OrderRepository::toOrder);
    }

    public List<Order> getOrdersByUserId(int userId) {
        return selectOrders()
                .where(ORDERS.ORDER_USER_ID.eq(userId))
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
     * Inserts the order and all of its items. The orderId and orderDate of the
     * given model are ignored; the database assigns them.
     *
     * @return the stored order, re-read so it carries the generated values.
     */
    public Order createOrder(Order order) {
        OrdersRecord created = jooq.insertInto(ORDERS)
                .set(ORDERS.ORDER_USER_ID, order.userId())
                .set(ORDERS.ORDER_TOTAL_AMOUNT, order.totalAmount())
                .set(ORDERS.ORDER_PAYMENT_DUE_DATE, order.paymentDueDate())
                .set(ORDERS.ORDER_PAYMENT_DATE, order.paymentDate())
                .set(ORDERS.ORDER_STATUS, toJooqStatus(order.status()))
                .returning()
                .fetchOne();

        int orderId = created.getOrderId();

        InsertValuesStep4<?, Integer, Integer, Integer, java.math.BigDecimal> insert =
                jooq.insertInto(ORDER_ITEMS,
                        ORDER_ITEMS.ORDER_ITEM_ORDER_ID,
                        ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID,
                        ORDER_ITEMS.ORDER_ITEM_QUANTITY,
                        ORDER_ITEMS.ORDER_ITEM_PRICE);

        for (OrderItem item : order.items()) {
            insert = insert.values(orderId, item.productId(), item.quantity(), item.unitPrice());
        }
        insert.execute();

        return getOrderById(orderId);
    }

    /** @return the updated order, or null if no order has that id. */
    public Order updateStatus(int orderId, OrderStatus status, LocalDateTime paymentDate) {
        int updated = jooq.update(ORDERS)
                .set(ORDERS.ORDER_STATUS, toJooqStatus(status))
                .set(ORDERS.ORDER_PAYMENT_DATE, paymentDate)
                .where(ORDERS.ORDER_ID.eq(orderId))
                .execute();

        return updated > 0 ? getOrderById(orderId) : null;
    }

    private org.jooq.SelectJoinStep<? extends Record> selectOrders() {
        return jooq.select(
                        ORDERS.ORDER_ID,
                        ORDERS.ORDER_USER_ID,
                        ORDERS.ORDER_TOTAL_AMOUNT,
                        ORDERS.ORDER_DATE,
                        ORDERS.ORDER_PAYMENT_DUE_DATE,
                        ORDERS.ORDER_PAYMENT_DATE,
                        ORDERS.ORDER_STATUS,
                        ITEMS)
                .from(ORDERS);
    }

    private static Order toOrder(Record record) {
        return new Order(
                record.get(ORDERS.ORDER_ID),
                record.get(ORDERS.ORDER_USER_ID),
                record.get(ITEMS),
                record.get(ORDERS.ORDER_TOTAL_AMOUNT),
                record.get(ORDERS.ORDER_DATE),
                record.get(ORDERS.ORDER_PAYMENT_DUE_DATE),
                record.get(ORDERS.ORDER_PAYMENT_DATE),
                fromJooqStatus(record.get(ORDERS.ORDER_STATUS))
        );
    }

    private static com.example.jooq.generated.enums.OrderStatus toJooqStatus(OrderStatus status) {
        return com.example.jooq.generated.enums.OrderStatus.valueOf(status.name());
    }

    // getLiteral(), not getName(): getName() returns the Postgres type name
    // ("order_status"), which would make valueOf() throw on every read.
    private static OrderStatus fromJooqStatus(com.example.jooq.generated.enums.OrderStatus status) {
        return OrderStatus.valueOf(status.getLiteral());
    }
}
