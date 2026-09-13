package com.example.service;

import com.example.dto.order.OrderItemRequest;
import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.OrderMapper;
import com.example.models.order.Order;
import com.example.models.order.OrderStatus;
import com.example.models.product.Product;
import com.example.models.ticket.Ticket;
import com.example.models.user.User;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import com.example.repository.SettingsRepository;
import com.example.security.CurrentUser;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class OrderService {

    /** How long the admin can still confirm a late payment after the customer's due date. */
    private static final int ADMIN_EXTRA_DAYS = 1;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    SettingsRepository settingsRepository;

    @Inject
    CurrentUser currentUser;

    @Inject
    MailService mailService;

    /**
     * Orders for the admin list, newest first.
     *
     * @param search order number, or any part of the customer's name or email; null for all
     * @param status only orders with this status; null for all
     */
    public List<OrderResponse> getOrders(String search, OrderStatus status) {
        return OrderMapper.toResponses(orderRepository.getOrders(search, status));
    }

    /** The order history of the logged-in customer, newest first. */
    public List<OrderResponse> getMyOrders() {
        User user = currentUser.get();
        return OrderMapper.toCustomerResponses(orderRepository.getOrdersByUserId(user.userId()), LocalDateTime.now());
    }

    public OrderResponse getOrderById(int orderId) {
        return OrderMapper.toResponse(findOrder(orderId));
    }

    /**
     * Places an order for the logged-in user, allocates its tickets until the
     * payment is due and emails the bank transfer details. Prices are read from
     * the products, never from the request. If one product has too few tickets
     * left, the transaction rolls back and nothing is allocated at all.
     *
     * @throws ResourceNotFoundException if the request names a product that does not exist.
     * @throws BusinessRuleException     if a product has too few tickets left.
     */
    public OrderResponse createOrder(OrderRequest request) {
        User user = currentUser.get();
        List<Ticket> tickets = new ArrayList<>();

        // Always lock the product rows in the same order, so two orders for the
        // same products cannot deadlock each other.
        List<OrderItemRequest> items = request.items().stream()
                .sorted(Comparator.comparingInt(OrderItemRequest::productId))
                .toList();

        for (OrderItemRequest item : items) {
            Product product = productRepository.getProductById(item.productId());
            if (product == null) {
                throw new ResourceNotFoundException("No product with id " + item.productId());
            }

            if (!productRepository.allocateTickets(product.productId(), item.quantity())) {
                throw new BusinessRuleException(
                        "Only " + product.availableTickets() + " tickets left for \"" + product.name() + "\"");
            }

            for (int i = 0; i < item.quantity(); i++) {
                tickets.add(new Ticket(0, newTicketCode(), product.productId(), product.name(), product.price()));
            }
        }

        // Ordered on 01.09 with 7 payment days: the customer has until 08.09 at midnight.
        LocalDateTime paymentDueAt = LocalDate.now()
                .plusDays(settingsRepository.getSettings().paymentDays())
                .atTime(23, 59, 59);

        Order order = orderRepository.createOrder(new Order(
                0,
                user,
                OrderStatus.PENDING,
                null,
                paymentDueAt,
                null,
                tickets
        ));

        mailService.sendPaymentInstructions(order);
        return OrderMapper.toCustomerResponse(order, LocalDateTime.now());
    }

    /**
     * Lets the logged-in customer cancel one of their orders, as long as it is unpaid and
     * its due date has not passed. The tickets are released right away.
     *
     * @throws ResourceNotFoundException if the order does not exist or belongs to someone else.
     * @throws BusinessRuleException     if the order can no longer be cancelled.
     */
    public OrderResponse cancelMyOrder(int orderId) {
        User user = currentUser.get();
        Order order = orderRepository.getOrderById(orderId);

        // Someone else's order is reported as missing, so order ids cannot be probed.
        if (order == null || order.user().userId() != user.userId()) {
            throw new ResourceNotFoundException("No order with id " + orderId);
        }

        if (order.customerStatus(LocalDateTime.now()) != OrderStatus.PENDING || !cancelAndReleaseTickets(order)) {
            throw new BusinessRuleException("Order " + orderId + " can no longer be cancelled");
        }

        return OrderMapper.toCustomerResponse(findOrder(orderId), LocalDateTime.now());
    }

    /**
     * Records the incoming bank transfer and emails the tickets as QR codes. A transfer
     * that arrives after the order was cancelled is still accepted, as long as enough
     * tickets are left to allocate them again.
     *
     * @throws BusinessRuleException if the order is already paid, or was cancelled and too
     *                               few tickets are left.
     */
    public OrderResponse markOrderAsPaid(int orderId) {
        Order order = findOrder(orderId);

        if (order.status() == OrderStatus.PAID) {
            throw new BusinessRuleException("Order " + orderId + " is already paid");
        }
        if (order.status() == OrderStatus.CANCELLED) {
            allocateTicketsAgain(order);
        }

        if (!orderRepository.markOrderAsPaid(orderId, order.status(), LocalDateTime.now())) {
            throw new BusinessRuleException("Order " + orderId + " was changed in the meantime, please try again");
        }

        Order paid = findOrder(orderId);
        mailService.sendTickets(paid);
        return OrderMapper.toResponse(paid);
    }

    /** Pending orders whose due date and the admin's extra day have both passed. */
    public List<Integer> getExpiredOrderIds() {
        return orderRepository.getOverdueOrderIds(LocalDateTime.now().minusDays(ADMIN_EXTRA_DAYS));
    }

    /**
     * Cancels an expired order, releases its tickets and tells the customer by email.
     * Called by {@link OrderExpiryJob}.
     */
    public void expireOrder(int orderId) {
        Order order = findOrder(orderId);

        // False if the order was paid in the meantime.
        if (!cancelAndReleaseTickets(order)) {
            return;
        }

        try {
            mailService.sendCancellation(order);
        } catch (RuntimeException e) {
            // The tickets have to be released either way, even if the address no longer exists.
            Log.errorf(e, "Could not send the cancellation email for order %d", orderId);
        }
    }

    private Order findOrder(int orderId) {
        Order order = orderRepository.getOrderById(orderId);

        if (order == null) {
            throw new ResourceNotFoundException("No order with id " + orderId);
        }
        return order;
    }

    /** @return false if the order was no longer pending. */
    private boolean cancelAndReleaseTickets(Order order) {
        if (!orderRepository.cancelOrder(order.orderId())) {
            return false;
        }
        ticketsPerProduct(order).forEach(productRepository::releaseTickets);
        return true;
    }

    private void allocateTicketsAgain(Order order) {
        ticketsPerProduct(order).forEach((productId, quantity) -> {
            if (!productRepository.allocateTickets(productId, quantity)) {
                throw new BusinessRuleException("Order " + order.orderId()
                        + " was cancelled and not enough tickets are left to confirm it");
            }
        });
    }

    /** Sorted by product id, so product rows are locked in the same order as in createOrder. */
    private static Map<Integer, Integer> ticketsPerProduct(Order order) {
        return order.tickets().stream()
                .collect(Collectors.groupingBy(Ticket::productId, TreeMap::new, Collectors.summingInt(ticket -> 1)));
    }

    /** 144 random bits, so a code cannot be guessed. URL safe, because the QR code holds a link. */
    private static String newTicketCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
