package com.example.controller;

import com.example.dto.product.ProductRequest;
import com.example.dto.product.ProductResponse;
import com.example.security.AdminPasswordProvider;
import com.example.service.ProductService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductController {
    @Inject
    ProductService productService;

    /**
     * Lists all ticket types with their prices and how many tickets are left.
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

    /**
     * Creates a ticket type. (This is only for the admin panel)
     */
    @POST
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public Response createProduct(@Valid ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Updates a ticket type. (This is only for the admin panel)
     */
    @PUT
    @Path("/{productId}")
    @RolesAllowed(AdminPasswordProvider.ADMIN_ROLE)
    public ProductResponse updateProductById(@PathParam("productId") int productId,
                                             @Valid ProductRequest request) {
        return productService.updateProductById(productId, request);
    }
}
