package com.example.service;

import com.example.dto.order.OrderRequest;
import com.example.dto.order.OrderResponse;
import com.example.models.order.Order;
import com.example.repository.OrderRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

// Info - takes orders from controller, talks to repo -> processes data from repo (use @Transactional)
@Transactional
public class OrderService {
    @Inject
    OrderRepository orderRepository;

    public List<Order> getOrders(){
        return orderRepository.getOrders();
    }


    public List<Order> getOrderByUId(int orderId){
        return orderRepository.getOrderByUId(orderId);
    }


    public Order getOrderById(int orderId){
        return orderRepository.getOrderById(orderId);
    }


    public OrderResponse createOrder(OrderRequest request){
        return orderRepository.createOrder(request);
    }


    public OrderResponse updateOrder(OrderRequest request){
        return null;
    }


    /**
        * Deletes an order by its ID.
        *
        * @param orderId The ID of the order to be deleted.
        * @return An OrderResponse indicating the result of the deletion operation.
    */
    public OrderResponse deleteOrderById(int orderId){
        return null;
    }

}
