package com.example.controller;

import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.service.OrderService;
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

// INFO - only receive HTTP requests and return HTTP responses
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderController {
    @Inject
    OrderService orderService;

    /**
     * Gets all orders. (This is only for the admin panel)
     *
     * @return a list of all orders
     */
    @GET
    public List<OrderResponse> getOrders() {
        return orderService.getOrders();
    }

    /**
     * Gets all orders for a specific user.
     *
     * @param userId the ID of the user of whom to retrieve orders
     */
    @GET
    @Path("/user/{userId}")
    public List<OrderResponse> getOrdersByUserId(@PathParam("userId") int userId) {
        return orderService.getOrdersByUserId(userId);
    }

    /**
     * Gets a specific order by its ID.
     *
     * @return the order, or 404 if it does not exist
     */
    @GET
    @Path("/{orderId}")
    public OrderResponse getOrderById(@PathParam("orderId") int orderId) {
        return orderService.getOrderById(orderId);
    }

    /**
     * Places an order. The total and the item prices are calculated server-side;
     * the payment due date is set to seven days from now.
     *
     * @return 201 with the stored order, 404 for an unknown product, 409 if stock ran out
     */
    @POST
    public Response createOrder(@Valid OrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Records the incoming bank transfer for an order. (This is only for the admin panel)
     */
    @PATCH
    @Path("/{orderId}/payment")
    public OrderResponse markOrderAsPaid(@PathParam("orderId") int orderId) {
        return orderService.markOrderAsPaid(orderId);
    }

    /**
     * Cancels an order and releases its reserved tickets. Orders are cancelled,
     * never deleted, so the history stays intact.
     */
    @PATCH
    @Path("/{orderId}/cancellation")
    public OrderResponse cancelOrderById(@PathParam("orderId") int orderId) {
        return orderService.cancelOrderById(orderId);
    }
}
