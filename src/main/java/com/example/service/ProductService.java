package com.example.service;

import com.example.dto.product.ProductRequest;
import com.example.dto.product.ProductResponse;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.ProductMapper;
import com.example.models.product.Product;
import com.example.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
@Transactional
public class ProductService {
    @Inject
    ProductRepository productRepository;

    public List<ProductResponse> getProducts() {
        return ProductMapper.toResponses(productRepository.getProducts());
    }

    public ProductResponse getProductById(int productId) {
        Product product = productRepository.getProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("No product with id " + productId);
        }
        return ProductMapper.toResponse(product);
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product created = productRepository.createProduct(ProductMapper.toModel(0, request));
        return ProductMapper.toResponse(created);
    }

    public ProductResponse updateProductById(int productId, ProductRequest request) {
        Product updated = productRepository.updateProduct(ProductMapper.toModel(productId, request));

        if (updated == null) {
            throw new ResourceNotFoundException("No product with id " + productId);
        }
        return ProductMapper.toResponse(updated);
    }
}
