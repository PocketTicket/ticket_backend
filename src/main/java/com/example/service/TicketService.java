package com.example.service;

import com.example.dto.ticket.TicketResponse;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.TicketMapper;
import com.example.models.order.Order;
import com.example.models.order.OrderItem;
import com.example.models.order.OrderStatus;
import com.example.models.product.Product;
import com.example.models.ticket.Ticket;
import com.example.repository.OrderRepository;
import com.example.repository.ProductRepository;
import com.example.repository.TicketRepository;
import com.example.ticket.QrCodeGenerator;
import com.example.ticket.TicketCodeGenerator;
import com.example.ticket.TicketValidationResult;
import com.example.ticket.TicketValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@Transactional
public class TicketService {

    /**
     * A 120 bit code colliding is not something that happens, but the column is
     * UNIQUE and an insert that trips it would roll back a paid order, so a few
     * cheap retries are worth more than the argument that they are unnecessary.
     */
    private static final int MAX_CODE_ATTEMPTS = 5;

    @Inject
    TicketRepository ticketRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    TicketCodeGenerator codeGenerator;

    @Inject
    TicketValidator ticketValidator;

    @Inject
    QrCodeGenerator qrCodeGenerator;

    public List<TicketResponse> getTicketsByOrderId(int orderId) {
        return TicketMapper.toResponses(ticketRepository.getTicketsByOrderId(orderId));
    }

    public TicketResponse getTicketByCode(String code) {
        return TicketMapper.toResponse(findTicket(code));
    }

    /** The PNG a guest shows at the door. */
    public byte[] getQrCode(String code) {
        Ticket ticket = findTicket(code);
        return qrCodeGenerator.toPng(ticket.code());
    }

    /**
     * Issues one ticket per admitted person: an item with quantity 3 becomes 3
     * tickets, because each is scanned separately. Called when an order is paid.
     *
     * <p>Does nothing if the order already has tickets, so marking an order paid
     * twice cannot double the tickets.
     */
    public List<TicketResponse> issueTicketsForOrder(Order order) {
        if (!ticketRepository.getTicketsByOrderId(order.orderId()).isEmpty()) {
            return getTicketsByOrderId(order.orderId());
        }

        for (OrderItem item : order.items()) {
            for (int i = 0; i < item.quantity(); i++) {
                ticketRepository.createTicket(order.orderId(), item.productId(), newUniqueCode());
            }
        }
        return getTicketsByOrderId(order.orderId());
    }

    /** Invalidates the tickets of a cancelled order. Used tickets keep their status. */
    public int cancelTicketsForOrder(int orderId) {
        return ticketRepository.cancelValidTicketsForOrder(orderId);
    }

    /**
     * Checks a scanned code and, if it passes, consumes the ticket so it cannot
     * be used again.
     *
     * @throws ResourceNotFoundException if the code belongs to no ticket
     * @throws BusinessRuleException     if the ticket is used, cancelled, unpaid or
     *                                   outside its entry window
     */
    public TicketResponse checkIn(String code) {
        Ticket ticket = findTicket(code);
        Product product = productRepository.getProductById(ticket.productId());
        OrderStatus orderStatus = orderRepository.getOrderStatus(ticket.orderId());

        TicketValidationResult result =
                ticketValidator.validate(ticket, product, orderStatus, LocalDateTime.now());

        if (!result.valid()) {
            throw new BusinessRuleException(result.message());
        }

        // The validation above ran on a ticket read a moment ago. This is the
        // write that actually settles it: if a second scanner got here first, the
        // conditional UPDATE matches nothing and only one of the two is let in.
        if (!ticketRepository.consume(code, LocalDateTime.now())) {
            throw new BusinessRuleException("Ticket was already used");
        }

        return TicketMapper.toResponse(ticketRepository.getTicketByCode(code));
    }

    private Ticket findTicket(String code) {
        Ticket ticket = ticketRepository.getTicketByCode(code);

        if (ticket == null) {
            throw new ResourceNotFoundException("No ticket with code " + code);
        }
        return ticket;
    }

    private String newUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.newCode();

            if (!ticketRepository.codeExists(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Could not generate a unique ticket code in " + MAX_CODE_ATTEMPTS + " attempts");
    }
}
