package com.example.mapper;

import com.example.dto.product.ProductRequest;
import com.example.dto.product.ProductResponse;
import com.example.models.product.Product;

import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    /** allocatedTickets is not client input; the repository never writes it from here. */
    public static Product toModel(int productId, ProductRequest request) {
        return new Product(
                productId,
                request.name(),
                request.description(),
                request.price(),
                request.location(),
                request.startsAt(),
                request.maxTickets(),
                0
        );
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.productId(),
                product.name(),
                product.description(),
                product.price(),
                product.location(),
                product.startsAt(),
                product.maxTickets(),
                product.availableTickets()
        );
    }

    public static List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(ProductMapper::toResponse).toList();
    }
}
