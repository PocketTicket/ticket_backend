package com.example.service;

import com.example.dto.product.ProductCreateRequest;
import com.example.dto.product.ProductUpdateRequest;
import com.example.models.product.Product;
import com.example.repository.ProductRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@Transactional
public class ProductService {
    @Inject
    ProductRepository productRepository;

    /**
     * Adds a product to database.
     * @return true on success, otherwise false.
     */

    public boolean addProduct(Product product){
        return productRepository.addProduct(product);
    }


    public List<Product> getProducts(){
        return productRepository.getProducts();
    }


    public Product getProductById(int productId){
        return productRepository.getProductById(productId);
    }


    public Product createProduct(ProductCreateRequest request){
        return productRepository.createProduct(request);
    }


    public Product updateProductById(int productId, ProductUpdateRequest request){
        return productRepository.updateProductById(productId, request);
    }


    /**
     * Deletes a product from database.
     * @param productId The ID of the product to be deleted.
     * @return true on success, otherwise false.
     */
    public boolean deleteProductById(int productId){
        return productRepository.deleteProductById(productId);
    }
}
