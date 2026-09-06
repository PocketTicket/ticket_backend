package com.example.service;

import com.example.dto.product.ProductCreateRequest;
import com.example.dto.product.ProductResponse;
import com.example.dto.product.ProductUpdateRequest;
import com.example.exception.BusinessRuleException;
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
        return ProductMapper.toResponse(findProduct(productId));
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        Product created = productRepository.createProduct(ProductMapper.toModel(request));
        return ProductMapper.toResponse(created);
    }

    public ProductResponse updateProductById(int productId, ProductUpdateRequest request) {
        Product updated = productRepository.updateProduct(ProductMapper.toModel(productId, request));

        if (updated == null) {
            throw new ResourceNotFoundException("No product with id " + productId);
        }
        return ProductMapper.toResponse(updated);
    }

    /**
     * Deletes a product.
     *
     * @throws ResourceNotFoundException if no product has that id.
     * @throws BusinessRuleException     if the product is part of an existing order.
     */
    public void deleteProductById(int productId) {
        findProduct(productId);

        if (productRepository.isReferencedByOrderItem(productId)) {
            throw new BusinessRuleException(
                    "Product " + productId + " belongs to at least one order and cannot be deleted. "
                            + "Set its stock to 0 instead.");
        }
        productRepository.deleteProductById(productId);
    }

    private Product findProduct(int productId) {
        Product product = productRepository.getProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("No product with id " + productId);
        }
        return product;
    }
}
