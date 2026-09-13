package com.example.mapper;

import com.example.dto.product.ProductCreateRequest;
import com.example.dto.product.ProductResponse;
import com.example.dto.product.ProductUpdateRequest;
import com.example.models.product.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    /** The id is assigned by the database, so it is left at 0 here. */
    public static Product toModel(ProductCreateRequest request) {
        return new Product(
                0,
                request.name(),
                request.description(),
                scale(request.price()),
                request.stock(),
                request.validFrom(),
                request.validUntil()
        );
    }

    public static Product toModel(int productId, ProductUpdateRequest request) {
        return new Product(
                productId,
                request.name(),
                request.description(),
                scale(request.price()),
                request.stock(),
                request.validFrom(),
                request.validUntil()
        );
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.productId(),
                product.name(),
                product.description(),
                product.price(),
                product.stock(),
                product.validFrom(),
                product.validUntil()
        );
    }

    public static List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(ProductMapper::toResponse).toList();
    }

    /** products.product_price is DECIMAL(10,2); match it so reads and writes agree. */
    private static BigDecimal scale(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
