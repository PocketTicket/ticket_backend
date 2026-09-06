package com.example.repository;

import com.example.models.product.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;

import static com.example.jooq.generated.Tables.ORDER_ITEMS;
import static com.example.jooq.generated.Tables.PRODUCTS;

@ApplicationScoped
public class ProductRepository {
    @Inject
    DSLContext jooq;

    public List<Product> getProducts() {
        return jooq.select(PRODUCTS.fields())
                .from(PRODUCTS)
                .orderBy(PRODUCTS.PRODUCT_ID)
                .fetch(ProductRepository::toProduct);
    }

    public Product getProductById(int productId) {
        return jooq.select(PRODUCTS.fields())
                .from(PRODUCTS)
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .fetchOne(ProductRepository::toProduct);
    }

    /** The productId of the given model is ignored; the database assigns it. */
    public Product createProduct(Product product) {
        return jooq.insertInto(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, product.name())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, product.description())
                .set(PRODUCTS.PRODUCT_PRICE, product.price())
                .set(PRODUCTS.PRODUCT_STOCK, product.stock())
                .returning()
                .fetchOne(ProductRepository::toProduct);
    }

    /** @return the updated product, or null if no product has that id. */
    public Product updateProduct(Product product) {
        return jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, product.name())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, product.description())
                .set(PRODUCTS.PRODUCT_PRICE, product.price())
                .set(PRODUCTS.PRODUCT_STOCK, product.stock())
                .where(PRODUCTS.PRODUCT_ID.eq(product.productId()))
                .returning()
                .fetchOne(ProductRepository::toProduct);
    }

    public boolean deleteProductById(int productId) {
        return jooq.deleteFrom(PRODUCTS)
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .execute() > 0;
    }

    /**
     * A product that appears in any order must not be deleted, otherwise the
     * order history loses its meaning. The FK enforces this too, but checking
     * up front lets the service answer with a proper 409 instead of a 500.
     */
    public boolean isReferencedByOrderItem(int productId) {
        return jooq.fetchExists(
                jooq.selectOne()
                        .from(ORDER_ITEMS)
                        .where(ORDER_ITEMS.ORDER_ITEM_PRODUCT_ID.eq(productId))
        );
    }

    /**
     * Reserves stock atomically: the WHERE clause makes the check and the
     * decrement a single statement, so two people ordering the last ticket at
     * the same time cannot both succeed.
     *
     * @return false if there was not enough stock left.
     */
    public boolean decreaseStock(int productId, int quantity) {
        return jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_STOCK, PRODUCTS.PRODUCT_STOCK.minus(quantity))
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .and(PRODUCTS.PRODUCT_STOCK.ge(quantity))
                .execute() > 0;
    }

    public void increaseStock(int productId, int quantity) {
        jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_STOCK, PRODUCTS.PRODUCT_STOCK.plus(quantity))
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .execute();
    }

    private static Product toProduct(Record record) {
        return new Product(
                record.get(PRODUCTS.PRODUCT_ID),
                record.get(PRODUCTS.PRODUCT_NAME),
                record.get(PRODUCTS.PRODUCT_DESCRIPTION),
                record.get(PRODUCTS.PRODUCT_PRICE),
                record.get(PRODUCTS.PRODUCT_STOCK)
        );
    }
}
