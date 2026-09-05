package com.example.controller;

import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.models.order.Order;
import com.example.service.OrderService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.util.List;

// INFO - only receive HTTP requests and return HTTP responses
public class OrderController {
    @Inject
    OrderService orderService;


    /**
     * Gets all orders. (This is only for the admin panel)
     * @return a list of all Orders without item describtion
     */
    @GET
    public List<Order> getOrders(){
        return orderService.getOrders();
    }


    /**
     * Gets all orders for a specific user
     * @param userId: The ID of the user of whom to retrieve orders
     * @return a list of all Orders for the specified user with item description
     */
    @GET
    @Path("/{userId}")
    public List<Order> getOrdersByUId(@PathParam("userId") int userId){
        return orderService.getOrderByUId(userId);
    }


    /**
     * Gets a specific order by its ID
     * @param orderId: The ID of the order
     * @return an Order
     */
    @GET
    @Path("/{orderId}")
    public Order getOrderById(@PathParam("orderId") int orderId){
        return orderService.getOrderById(orderId);
    }


    // sets the paymentdate to LocalDateTime.now().plusDays(7)
    @POST
    public OrderResponse createOrder(OrderRequest request){
        return orderService.createOrder(request);
    }


    @PUT
    public OrderResponse deleteOrderById(int orderId){
        return orderService.deleteOrderById(orderId);
    }
}
