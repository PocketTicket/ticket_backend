package com.example.service;

import com.example.dto.order.OrderItemRequest;
import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.OrderMapper;
import com.example.models.order.Order;
import com.example.models.order.OrderItem;
import com.example.models.order.OrderStatus;
import com.example.models.product.Product;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// INFO - takes DTOs from the controller, applies business logic, talks to the
// repositories in models, and converts the result back into a safe DTO.
@ApplicationScoped
@Transactional
public class OrderService {

    /** How long a reservation stays open before the bank transfer is due. */
    private static final int PAYMENT_WINDOW_DAYS = 7;

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    /** Every order in the system. Intended for the admin panel. */
    public List<OrderResponse> getOrders() {
        return OrderMapper.toResponses(orderRepository.getOrders());
    }

    public List<OrderResponse> getOrdersByUserId(int userId) {
        return OrderMapper.toResponses(orderRepository.getOrdersByUserId(userId));
    }

    public OrderResponse getOrderById(int orderId) {
        return OrderMapper.toResponse(findOrder(orderId));
    }

    /**
     * Places an order. Prices and the total are read from the products, never from
     * the request, and the stock of every product is reserved in the same
     * transaction - so an order either exists with its tickets held, or not at all.
     *
     * @throws ResourceNotFoundException if the request names a product that does not exist.
     * @throws BusinessRuleException     if a product does not have enough stock left.
     */
    public OrderResponse createOrder(OrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Integer, Integer> entry : mergeQuantitiesByProduct(request.items()).entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.getProductById(productId);
            if (product == null) {
                throw new ResourceNotFoundException("No product with id " + productId);
            }

            if (!productRepository.decreaseStock(productId, quantity)) {
                throw new BusinessRuleException(
                        "Only " + product.stock() + " left of \"" + product.name()
                                + "\", but " + quantity + " were requested");
            }

            // orderItemId is assigned by the database on insert.
            OrderItem item = new OrderItem(0, productId, product.name(), quantity, product.price());
            items.add(item);
            total = total.add(item.lineTotal());
        }

        Order order = new Order(
                0,
                request.userId(),
                items,
                total,
                null,
                LocalDateTime.now().plusDays(PAYMENT_WINDOW_DAYS),
                null,
                OrderStatus.ORDERED
        );

        return OrderMapper.toResponse(orderRepository.createOrder(order));
    }

    /**
     * Records an incoming bank transfer. Only an open order can be paid.
     *
     * @throws BusinessRuleException if the order is not in state ORDERED.
     */
    public OrderResponse markOrderAsPaid(int orderId) {
        Order order = findOrder(orderId);

        if (order.status() != OrderStatus.ORDERED) {
            throw new BusinessRuleException(
                    "Order " + orderId + " is " + order.status() + " and cannot be marked as paid");
        }

        return OrderMapper.toResponse(
                orderRepository.updateStatus(orderId, OrderStatus.PAID, LocalDateTime.now()));
    }

    /**
     * Cancels an order and releases the reserved stock. Orders are never deleted:
     * the row stays as a record of what happened, which is also what the admin
     * panel and any later refund handling need.
     *
     * @throws BusinessRuleException if the order was already handed out or closed.
     */
    public OrderResponse cancelOrderById(int orderId) {
        Order order = findOrder(orderId);

        if (order.status() == OrderStatus.CANCELLED || order.status() == OrderStatus.REVOKED) {
            throw new BusinessRuleException("Order " + orderId + " is already " + order.status());
        }
        if (order.status() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException(
                    "Order " + orderId + " was already delivered and cannot be cancelled");
        }

        for (OrderItem item : order.items()) {
            productRepository.increaseStock(item.productId(), item.quantity());
        }

        return OrderMapper.toResponse(
                orderRepository.updateStatus(orderId, OrderStatus.CANCELLED, order.paymentDate()));
    }

    private Order findOrder(int orderId) {
        Order order = orderRepository.getOrderById(orderId);

        if (order == null) {
            throw new ResourceNotFoundException("No order with id " + orderId);
        }
        return order;
    }

    /**
     * The same product may appear twice in a cart. Collapsing it here keeps one
     * order_items row per product and makes the stock reservation below check the
     * full quantity at once instead of twice against a stale value.
     */
    private static Map<Integer, Integer> mergeQuantitiesByProduct(List<OrderItemRequest> items) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();

        for (OrderItemRequest item : items) {
            quantities.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantities;
    }
}
