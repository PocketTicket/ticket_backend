package com.example.repository;

import com.example.models.product.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;

import static com.example.jooq.generated.Tables.PRODUCTS;

@ApplicationScoped
public class ProductRepository {
    @Inject
    DSLContext jooq;

    public List<Product> getProducts() {
        return jooq.selectFrom(PRODUCTS)
                .orderBy(PRODUCTS.PRODUCT_STARTS_AT, PRODUCTS.PRODUCT_ID)
                .fetch(ProductRepository::toProduct);
    }

    /** @return the product, or null if no product has that id. */
    public Product getProductById(int productId) {
        return jooq.selectFrom(PRODUCTS)
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .fetchOne(ProductRepository::toProduct);
    }

    /** The productId and allocatedTickets of the given model are ignored. */
    public Product createProduct(Product product) {
        return jooq.insertInto(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, product.name())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, product.description())
                .set(PRODUCTS.PRODUCT_PRICE, product.price())
                .set(PRODUCTS.PRODUCT_LOCATION, product.location())
                .set(PRODUCTS.PRODUCT_STARTS_AT, product.startsAt())
                .set(PRODUCTS.PRODUCT_MAX_TICKETS, product.maxTickets())
                .returning()
                .fetchOne(ProductRepository::toProduct);
    }

    /**
     * The allocatedTickets of the given model are ignored.
     *
     * @return the updated product, or null if no product has that id.
     */
    public Product updateProduct(Product product) {
        return jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, product.name())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, product.description())
                .set(PRODUCTS.PRODUCT_PRICE, product.price())
                .set(PRODUCTS.PRODUCT_LOCATION, product.location())
                .set(PRODUCTS.PRODUCT_STARTS_AT, product.startsAt())
                .set(PRODUCTS.PRODUCT_MAX_TICKETS, product.maxTickets())
                .where(PRODUCTS.PRODUCT_ID.eq(product.productId()))
                .returning()
                .fetchOne(ProductRepository::toProduct);
    }

    /**
     * Allocates tickets if enough are left. Check and increment are one statement,
     * so two people buying the last ticket at the same moment cannot both get it.
     *
     * @return false if fewer than {@code quantity} tickets are left.
     */
    public boolean allocateTickets(int productId, int quantity) {
        return jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_ALLOCATED_TICKETS, PRODUCTS.PRODUCT_ALLOCATED_TICKETS.plus(quantity))
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .and(PRODUCTS.PRODUCT_ALLOCATED_TICKETS.plus(quantity).le(PRODUCTS.PRODUCT_MAX_TICKETS))
                .execute() > 0;
    }

    private static Product toProduct(Record record) {
        return new Product(
                record.get(PRODUCTS.PRODUCT_ID),
                record.get(PRODUCTS.PRODUCT_NAME),
                record.get(PRODUCTS.PRODUCT_DESCRIPTION),
                record.get(PRODUCTS.PRODUCT_PRICE),
                record.get(PRODUCTS.PRODUCT_LOCATION),
                record.get(PRODUCTS.PRODUCT_STARTS_AT),
                record.get(PRODUCTS.PRODUCT_MAX_TICKETS),
                record.get(PRODUCTS.PRODUCT_ALLOCATED_TICKETS)
        );
    }
}
