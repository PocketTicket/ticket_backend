package com.example.repository;

import com.example.dto.order.OrderItemRequest;
import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.models.order.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.models.order.OrderStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import static com.example.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

// INFO - only communication with database (e.g. SQL's)
@ApplicationScoped
public class OrderRepository {
    @Inject
    DSLContext jooq;

    public List<Order> getOrders(){
        return jooq.select()
                .from(ORDERS)
                .fetch(record -> new Order(
                        record.get(ORDERS.ORDER_ID),
                        record.get(ORDERS.ORDER_USER_ID),
                        null,
                        record.get(ORDERS.ORDER_TOTAL_AMOUNT).doubleValue(),
                        record.get(ORDERS.ORDER_DATE),
                        record.get(ORDERS.ORDER_PAYMENT_DUE_DATE),
                        record.get(ORDERS.ORDER_PAYMENT_DATE),
                        OrderStatus.valueOf(record.get(ORDERS.ORDER_STATUS).getName())
                ));
    }

    public List<Order> getOrderByUId(int userId) {
        return jooq.select(
                        ORDERS.ORDER_ID,
                        ORDERS.ORDER_USER_ID,
                        ORDERS.ORDER_TOTAL_AMOUNT,
                        ORDERS.ORDER_DATE,
                        ORDERS.ORDER_PAYMENT_DUE_DATE,
                        ORDERS.ORDER_PAYMENT_DATE,
                        ORDERS.ORDER_STATUS,
                        multiset(select(
                                ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID,
                                ORDER_ITEMS.ORDER_ITEM_QUANTITY,
                                ORDER_ITEMS.ORDER_ITEM_PRICE)
                                .from(ORDER_ITEMS)
                                .where(ORDER_ITEMS.ORDER_ITEM_ORDER_ID.eq(ORDERS.ORDER_ID))
                        ).as("items")
                                .convertFrom(result -> result.map(r ->
                                        new OrderItemRequest(
                                                r.get(ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID),
                                                r.get(ORDER_ITEMS.ORDER_ITEM_QUANTITY),
                                                r.get(ORDER_ITEMS.ORDER_ITEM_PRICE).doubleValue()
                                        )
                                ))
                )
                .from(ORDERS)
                .where(ORDERS.ORDER_USER_ID.eq(userId))
                .fetch(record -> new Order(
                        record.get(ORDERS.ORDER_ID),
                        record.get(ORDERS.ORDER_USER_ID),
                        record.get("items", List.class),
                        record.get(ORDERS.ORDER_TOTAL_AMOUNT).doubleValue(),
                        record.get(ORDERS.ORDER_DATE),
                        record.get(ORDERS.ORDER_PAYMENT_DUE_DATE),
                        record.get(ORDERS.ORDER_PAYMENT_DATE),
                        OrderStatus.valueOf(record.get(ORDERS.ORDER_STATUS).getName())
                ));
    }


    public Order getOrderById(int orderId){
        return jooq.select(
                        ORDERS.ORDER_ID,
                        ORDERS.ORDER_USER_ID,
                        ORDERS.ORDER_TOTAL_AMOUNT,
                        ORDERS.ORDER_DATE,
                        ORDERS.ORDER_PAYMENT_DUE_DATE,
                        ORDERS.ORDER_PAYMENT_DATE,
                        ORDERS.ORDER_STATUS,
                        multiset(select(
                                ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID,
                                ORDER_ITEMS.ORDER_ITEM_QUANTITY,
                                ORDER_ITEMS.ORDER_ITEM_PRICE)
                                .from(ORDER_ITEMS)
                                .where(ORDER_ITEMS.ORDER_ITEM_ORDER_ID.eq(ORDERS.ORDER_ID))
                        ).as("items")
                                .convertFrom(result -> result.map(r ->
                                        new OrderItemRequest(
                                                r.get(ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID),
                                                r.get(ORDER_ITEMS.ORDER_ITEM_QUANTITY),
                                                r.get(ORDER_ITEMS.ORDER_ITEM_PRICE).doubleValue()
                                        )
                                ))
                )
                .from(ORDERS)
                .where(ORDERS.ORDER_ID.eq(orderId))
                .fetchOne(record -> new Order(
                        record.get(ORDERS.ORDER_ID),
                        record.get(ORDERS.ORDER_USER_ID),
                        record.get("items", List.class),
                        record.get(ORDERS.ORDER_TOTAL_AMOUNT).doubleValue(),
                        record.get(ORDERS.ORDER_DATE),
                        record.get(ORDERS.ORDER_PAYMENT_DUE_DATE),
                        record.get(ORDERS.ORDER_PAYMENT_DATE),
                        OrderStatus.valueOf(record.get(ORDERS.ORDER_STATUS).getName())
                ));
    }


    public OrderResponse createOrder(OrderRequest request){
        return jooq.insertInto(ORDERS)
                .set(ORDERS.ORDER_USER_ID, request.userId())
                .set(ORDERS.ORDER_TOTAL_AMOUNT, BigDecimal.valueOf(request.total()))
                .set(ORDERS.ORDER_PAYMENT_DUE_DATE, LocalDateTime.now().plusDays(7))
                .set(ORDERS.ORDER_STATUS, com.example.jooq.generated.enums.OrderStatus.ORDERED)
                .returning()
                .fetchOne(record -> new OrderResponse(
                        record.get(ORDERS.ORDER_ID),
                        record.get(ORDERS.ORDER_USER_ID),
                        request.items(),
                        record.get(ORDERS.ORDER_TOTAL_AMOUNT).doubleValue(),
                        record.get(ORDERS.ORDER_DATE),
                        record.get(ORDERS.ORDER_PAYMENT_DUE_DATE),
                        OrderStatus.valueOf(record.get(ORDERS.ORDER_STATUS).getName())
                ));
    }

}
