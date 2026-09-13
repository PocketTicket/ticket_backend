package com.example.controller;

import com.example.dto.ticket.TicketResponse;
import com.example.service.TicketService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/tickets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TicketController {
    @Inject
    TicketService ticketService;

    /** The tickets issued for one order, i.e. what goes into the confirmation email. */
    @GET
    @Path("/order/{orderId}")
    public List<TicketResponse> getTicketsByOrderId(@PathParam("orderId") int orderId) {
        return ticketService.getTicketsByOrderId(orderId);
    }

    /**
     * Looks a ticket up without consuming it, so the door staff can see what a
     * code is before deciding to check it in.
     */
    @GET
    @Path("/{code}")
    public TicketResponse getTicketByCode(@PathParam("code") String code) {
        return ticketService.getTicketByCode(code);
    }

    /** The QR image itself, for the email attachment or the guest's phone. */
    @GET
    @Path("/{code}/qr")
    @Produces("image/png")
    public Response getQrCode(@PathParam("code") String code) {
        return Response.ok(ticketService.getQrCode(code))
                .header("Cache-Control", "no-store")
                .build();
    }

    /**
     * Admits a guest: validates the scanned code and marks the ticket as used in
     * one step, so the same code cannot get two people through the door.
     *
     * @return 200 with the used ticket, 404 for an unknown code, 409 with the
     *         reason if the ticket is used, cancelled, unpaid or out of its window
     */
    @POST
    @Path("/{code}/check-in")
    public TicketResponse checkIn(@PathParam("code") String code) {
        return ticketService.checkIn(code);
    }
}
