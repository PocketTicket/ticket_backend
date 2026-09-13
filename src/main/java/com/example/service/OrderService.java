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
import com.example.security.CurrentUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
@Transactional
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @ConfigProperty(name = "ticket.payment-days")
    int paymentDays;

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    CurrentUser currentUser;

    @Inject
    MailService mailService;

    /** Every order in the system. Intended for the admin panel. */
    public List<OrderResponse> getOrders() {
        return OrderMapper.toResponses(orderRepository.getOrders());
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

        Order order = orderRepository.createOrder(new Order(
                0,
                user,
                OrderStatus.PENDING,
                null,
                LocalDateTime.now().plusDays(paymentDays),
                null,
                tickets
        ));

        mailService.sendPaymentInstructions(order);
        return OrderMapper.toResponse(order);
    }

    /**
     * Records the incoming bank transfer and emails the tickets as QR codes.
     *
     * @throws BusinessRuleException if the order is not waiting for payment.
     */
    public OrderResponse markOrderAsPaid(int orderId) {
        Order order = findOrder(orderId);

        if (!orderRepository.markOrderAsPaid(orderId, LocalDateTime.now())) {
            throw new BusinessRuleException(
                    "Order " + orderId + " is " + order.status() + " and cannot be marked as paid");
        }

        Order paid = findOrder(orderId);
        mailService.sendTickets(paid);
        return OrderMapper.toResponse(paid);
    }

    private Order findOrder(int orderId) {
        Order order = orderRepository.getOrderById(orderId);

        if (order == null) {
            throw new ResourceNotFoundException("No order with id " + orderId);
        }
        return order;
    }

    /** 144 random bits, so a code cannot be guessed. URL safe, because the QR code holds a link. */
    private static String newTicketCode() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
