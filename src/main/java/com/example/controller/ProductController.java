package com.example.controller;

import com.example.dto.product.ProductCreateRequest;
import com.example.dto.product.ProductResponse;
import com.example.dto.product.ProductUpdateRequest;
import com.example.service.ProductService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

// INFO - only receive HTTP requests and return HTTP responses
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {
    @Inject
    ProductService productService;

    /**
     * Lists everything that can be ordered.
     */
    @GET
    public List<ProductResponse> getProducts() {
        return productService.getProducts();
    }

    /**
     * @param productId the ID of the product
     * @return the product, or 404 if it does not exist
     */
    @GET
    @Path("/{productId}")
    public ProductResponse getProductById(@PathParam("productId") int productId) {
        return productService.getProductById(productId);
    }

    @POST
    public Response createProduct(@Valid ProductCreateRequest request) {
        ProductResponse created = productService.createProduct(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{productId}")
    public ProductResponse updateProductById(@PathParam("productId") int productId,
                                             @Valid ProductUpdateRequest request) {
        return productService.updateProductById(productId, request);
    }

    /**
     * @return 204 on success, 404 if unknown, 409 if the product is part of an order
     */
    @DELETE
    @Path("/{productId}")
    public Response deleteProductById(@PathParam("productId") int productId) {
        productService.deleteProductById(productId);
        return Response.noContent().build();
    }
}
