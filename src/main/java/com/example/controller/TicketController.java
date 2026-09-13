package com.example.controller;

import com.example.dto.ticket.TicketCheckResponse;
import com.example.security.AdminPasswordProvider;
import com.example.service.TicketService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The page behind a ticket's QR code ({ticket.frontend-url}/tickets/{code}) first asks
 * GET /admin/me: the door staff account and admins with the setup done get
 * POST .../check-in; everyone else gets GET, which never uses the ticket up. Both always
 * answer 200 with a result.
 */
@Path("/tickets")
@Produces(MediaType.APPLICATION_JSON)
public class TicketController {
    @Inject
    TicketService ticketService;

    /**
     * Shows whether a ticket is valid, e.g. for a customer checking their own ticket.
     * Never uses the ticket up, so a photographed QR code cannot invalidate it.
     */
    @GET
    @Path("/{code}")
    public TicketCheckResponse checkTicket(@PathParam("code") String code) {
        return ticketService.checkTicket(code);
    }

    /**
     * The door check: lets the guest in and uses the ticket up if it is valid right now.
     * (Only for the door staff account and admins)
     */
    @POST
    @Path("/{code}/check-in")
    @RolesAllowed(AdminPasswordProvider.DOOR_ROLE)
    public TicketCheckResponse checkIn(@PathParam("code") String code) {
        return ticketService.checkIn(code);
    }
}
