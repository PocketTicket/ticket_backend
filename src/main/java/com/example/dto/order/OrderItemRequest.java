package com.example.dto.order;

public record OrderItemRequest(
   int productId,
   int quantity,
   double price
){ }