package com.example.controller;

import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.models.order.OrderStatus;
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
import jakarta.ws.rs.QueryParam;
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
     * Gets the orders for the admin list, newest first. (This is only for the admin panel)
     *
     * @param search optional: the order number, or any part of the customer's name or email
     * @param status optional: PENDING, PAID or CANCELLED
     */
    @GET
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public List<OrderResponse> getOrders(@QueryParam("search") String search,
                                         @QueryParam("status") OrderStatus status) {
        return orderService.getOrders(search, status);
    }

    /**
     * The order history of the logged-in customer, newest first. An unpaid order whose
     * due date has passed is shown as CANCELLED.
     *
     * @return the orders, or 401 if not logged in
     */
    @GET
    @Path("/mine")
    public List<OrderResponse> getMyOrders() {
        return orderService.getMyOrders();
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
     * Lets the logged-in customer cancel one of their unpaid orders before its due date.
     *
     * @return the cancelled order, 401 if not logged in, 404 if the order is not theirs,
     *         409 if it is paid, cancelled or past its due date
     */
    @PATCH
    @Path("/{orderId}/cancellation")
    public OrderResponse cancelMyOrder(@PathParam("orderId") int orderId) {
        return orderService.cancelMyOrder(orderId);
    }

    /**
     * Confirms that the bank transfer arrived and emails the tickets as QR codes. Also
     * works for a cancelled order if enough tickets are left. (This is only for the admin panel)
     *
     * @return the paid order, 404 if unknown, 409 if already paid or too few tickets are left
     */
    @PATCH
    @Path("/{orderId}/payment")
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public OrderResponse markOrderAsPaid(@PathParam("orderId") int orderId) {
        return orderService.markOrderAsPaid(orderId);
    }
}
