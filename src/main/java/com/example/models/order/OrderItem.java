package com.example.models.order;

import java.math.BigDecimal;

// INFO - domain model, mirrors a row of order_items. Knows nothing about HTTP or DTOs.
public record OrderItem(
        int orderItemId,
        int productId,
        // Joined from products for display; the price below is the snapshot taken
        // at order time and must NOT be re-read from the product.
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
