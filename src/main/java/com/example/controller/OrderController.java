package com.example.controller;

import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.security.AdminPasswordProvider;
import com.example.service.OrderService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderController {
    @Inject
    OrderService orderService;

    /**
     * Gets all orders. (This is only for the admin panel)
     */
    @GET
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public List<OrderResponse> getOrders() {
        return orderService.getOrders();
    }

    /**
     * Gets a specific order by its ID. (This is only for the admin panel)
     *
     * @return the order, or 404 if it does not exist
     */
    @GET
    @Path("/{orderId}")
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public OrderResponse getOrderById(@PathParam("orderId") int orderId) {
        return orderService.getOrderById(orderId);
    }

    /**
     * Places an order for the logged-in user. The tickets are reserved until the
     * payment is due and the user gets an email with the bank transfer details.
     *
     * @return 201 with the order, 401 if not logged in, 404 for an unknown product,
     *         409 if too few tickets are left
     */
    @POST
    public Response createOrder(@Valid OrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Confirms that the bank transfer arrived and emails the tickets as QR codes.
     * (This is only for the admin panel)
     *
     * @return the paid order, 404 if unknown, 409 if the order is not pending
     */
    @PATCH
    @Path("/{orderId}/payment")
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public OrderResponse markOrderAsPaid(@PathParam("orderId") int orderId) {
        return orderService.markOrderAsPaid(orderId);
    }
}
