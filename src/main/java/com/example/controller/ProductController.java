package com.example.controller;

import com.example.dto.product.ProductCreateRequest;
import com.example.dto.product.ProductUpdateRequest;
import com.example.models.product.Product;
import com.example.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.util.List;

public class ProductController {
    @Inject
    ProductService productService;


    @GET
    public List<Product> getProducts(){
        return productService.getProducts();
    }


    @GET
    @Path("/{productId}")
    public Product getProductById(@PathParam("productId") int productId){
        return productService.getProductById(productId);
    }


    @POST
    public Product createProduct(ProductCreateRequest request){
        return productService.createProduct(request);
    }


    @PUT
    @Path("/{productId}")
    public Product updateProductById(@PathParam("productId") int productId, ProductUpdateRequest request){
        return productService.updateProductById(productId, request);
    }


    @DELETE
    public boolean deleteProductById(int productId){
        return productService.deleteProductById(productId);
    }
}
