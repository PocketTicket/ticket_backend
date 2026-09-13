package com.example.mapper;

import com.example.dto.product.ProductImageResponse;
import com.example.dto.product.ProductRequest;
import com.example.dto.product.ProductResponse;
import com.example.models.product.Product;
import com.example.models.product.ProductImage;

import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    /** allocatedTickets and hasImage are not client input; the repository never writes them from here. */
    public static Product toModel(int productId, ProductRequest request) {
        return new Product(
                productId,
                request.name(),
                request.description(),
                request.price(),
                request.location(),
                request.startsAt(),
                request.maxTickets(),
                0,
                false
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
                product.availableTickets(),
                product.hasImage()
        );
    }

    public static List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(ProductMapper::toResponse).toList();
    }

    public static ProductImageResponse toImageResponse(ProductImage image) {
        return new ProductImageResponse(image.contentType(), image.data());
    }
}
