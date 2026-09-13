package com.example.repository;

import com.example.models.ticket.Ticket;
import com.example.models.ticket.TicketStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectJoinStep;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.jooq.generated.Tables.PRODUCTS;
import static com.example.jooq.generated.Tables.TICKETS;

@ApplicationScoped
public class TicketRepository {
    @Inject
    DSLContext jooq;

    public List<Ticket> getTicketsByOrderId(int orderId) {
        return selectTickets()
                .where(TICKETS.TICKET_ORDER_ID.eq(orderId))
                .orderBy(TICKETS.TICKET_ID)
                .fetch(TicketRepository::toTicket);
    }

    /** @return the ticket, or null if the code belongs to no ticket. */
    public Ticket getTicketByCode(String code) {
        return selectTickets()
                .where(TICKETS.TICKET_CODE.eq(code))
                .fetchOne(TicketRepository::toTicket);
    }

    public boolean codeExists(String code) {
        return jooq.fetchExists(
                jooq.selectOne().from(TICKETS).where(TICKETS.TICKET_CODE.eq(code)));
    }

    public void createTicket(int orderId, int productId, String code) {
        jooq.insertInto(TICKETS)
                .set(TICKETS.TICKET_ORDER_ID, orderId)
                .set(TICKETS.TICKET_PRODUCT_ID, productId)
                .set(TICKETS.TICKET_CODE, code)
                .set(TICKETS.TICKET_STATUS, com.example.jooq.generated.enums.TicketStatus.VALID)
                .execute();
    }

    /**
     * Marks the ticket as used, but only if it still was valid. Doing the check
     * and the write in one statement is what stops two doors scanning the same
     * code at the same moment from both letting someone in.
     *
     * @return false if the ticket was already used or cancelled in the meantime.
     */
    public boolean consume(String code, LocalDateTime usedAt) {
        return jooq.update(TICKETS)
                .set(TICKETS.TICKET_STATUS, com.example.jooq.generated.enums.TicketStatus.USED)
                .set(TICKETS.TICKET_USED_AT, usedAt)
                .where(TICKETS.TICKET_CODE.eq(code))
                .and(TICKETS.TICKET_STATUS.eq(com.example.jooq.generated.enums.TicketStatus.VALID))
                .execute() > 0;
    }

    /** Used tickets keep their status: someone did walk in with them. */
    public int cancelValidTicketsForOrder(int orderId) {
        return jooq.update(TICKETS)
                .set(TICKETS.TICKET_STATUS, com.example.jooq.generated.enums.TicketStatus.CANCELLED)
                .where(TICKETS.TICKET_ORDER_ID.eq(orderId))
                .and(TICKETS.TICKET_STATUS.eq(com.example.jooq.generated.enums.TicketStatus.VALID))
                .execute();
    }

    private SelectJoinStep<? extends Record> selectTickets() {
        return jooq.select(
                        TICKETS.TICKET_ID,
                        TICKETS.TICKET_CODE,
                        TICKETS.TICKET_ORDER_ID,
                        TICKETS.TICKET_PRODUCT_ID,
                        PRODUCTS.PRODUCT_NAME,
                        TICKETS.TICKET_STATUS,
                        TICKETS.TICKET_ISSUED_AT,
                        TICKETS.TICKET_USED_AT)
                .from(TICKETS)
                .join(PRODUCTS).on(PRODUCTS.PRODUCT_ID.eq(TICKETS.TICKET_PRODUCT_ID));
    }

    private static Ticket toTicket(Record record) {
        return new Ticket(
                record.get(TICKETS.TICKET_ID),
                record.get(TICKETS.TICKET_CODE),
                record.get(TICKETS.TICKET_ORDER_ID),
                record.get(TICKETS.TICKET_PRODUCT_ID),
                record.get(PRODUCTS.PRODUCT_NAME),
                TicketStatus.valueOf(record.get(TICKETS.TICKET_STATUS).getLiteral()),
                record.get(TICKETS.TICKET_ISSUED_AT),
                record.get(TICKETS.TICKET_USED_AT)
        );
    }
}
