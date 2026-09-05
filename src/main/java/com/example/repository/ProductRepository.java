package com.example.repository;

import com.example.dto.product.ProductCreateRequest;
import com.example.dto.product.ProductUpdateRequest;
import com.example.models.product.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;

import java.math.BigDecimal;
import java.util.List;

import static com.example.jooq.generated.Tables.*;

@ApplicationScoped
public class ProductRepository {
    @Inject
    DSLContext jooq;

    public boolean addProduct(Product product){
        return jooq.insertInto(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, product.productName())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, product.productDescription())
                .set(PRODUCTS.PRODUCT_PRICE, BigDecimal.valueOf(product.productPrice()))
                .set(PRODUCTS.PRODUCT_STOCK, product.productStock())
                .execute() > 0;
    }


    public List<Product> getProducts(){
        return jooq.select()
                .from(PRODUCTS)
                .fetchInto(Product.class);
    }


    public Product getProductById(int productId){
        return jooq.select()
                .from(PRODUCTS)
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .fetchOneInto(Product.class);
    }


    public Product createProduct(ProductCreateRequest request){
        return jooq.insertInto(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, request.productName())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, request.productDescription())
                .set(PRODUCTS.PRODUCT_PRICE, BigDecimal.valueOf(request.productPrice()))
                .set(PRODUCTS.PRODUCT_STOCK, request.productStock())
                .returning()
                .fetchOneInto(Product.class);
    }


    public Product updateProductById(int productId, ProductUpdateRequest request){
        return jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, request.productName())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, request.productDescription())
                .set(PRODUCTS.PRODUCT_PRICE, BigDecimal.valueOf(request.productPrice()))
                .set(PRODUCTS.PRODUCT_STOCK, request.productStock())
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .returning()
                .fetchOneInto(Product.class);
    }


    public boolean deleteProductById(int productId){
        return jooq.deleteFrom(PRODUCTS)
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .execute() > 0;
    }
}
