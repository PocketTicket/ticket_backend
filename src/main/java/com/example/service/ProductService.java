package com.example.service;

import com.example.dto.product.ProductImageResponse;
import com.example.dto.product.ProductRequest;
import com.example.dto.product.ProductResponse;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.ProductMapper;
import com.example.models.product.Product;
import com.example.models.product.ProductImage;
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

    /**
     * @throws ResourceNotFoundException if the product has no picture.
     */
    public ProductImageResponse getImage(int productId) {
        ProductImage image = productRepository.getImage(productId);

        if (image == null) {
            throw new ResourceNotFoundException("Product " + productId + " has no image");
        }
        return ProductMapper.toImageResponse(image);
    }

    /**
     * Stores the picture of a product, replacing any previous one.
     *
     * @throws ResourceNotFoundException if no product has that id.
     */
    public void saveImage(int productId, String contentType, byte[] data) {
        findProduct(productId);
        productRepository.saveImage(productId, new ProductImage(contentType, data));
    }

    private Product findProduct(int productId) {
        Product product = productRepository.getProductById(productId);

        if (product == null) {
            throw new ResourceNotFoundException("No product with id " + productId);
        }
        return product;
    }
}
