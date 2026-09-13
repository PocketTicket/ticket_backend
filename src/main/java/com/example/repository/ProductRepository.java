package com.example.repository;

import com.example.models.product.Product;
import com.example.models.product.ProductImage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;

import java.util.List;

import static com.example.jooq.generated.Tables.PRODUCTS;
import static com.example.jooq.generated.Tables.PRODUCT_IMAGES;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.selectOne;

@ApplicationScoped
public class ProductRepository {

    /** Whether the product has a picture, without loading the picture itself. */
    private static final Field<Boolean> HAS_IMAGE = field(exists(
            selectOne()
                    .from(PRODUCT_IMAGES)
                    .where(PRODUCT_IMAGES.PRODUCT_IMAGE_PRODUCT_ID.eq(PRODUCTS.PRODUCT_ID))))
            .as("has_image");

    @Inject
    DSLContext jooq;

    public List<Product> getProducts() {
        return selectProducts()
                .orderBy(PRODUCTS.PRODUCT_STARTS_AT, PRODUCTS.PRODUCT_ID)
                .fetch(ProductRepository::toProduct);
    }

    /** @return the product, or null if no product has that id. */
    public Product getProductById(int productId) {
        return selectProducts()
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .fetchOne(ProductRepository::toProduct);
    }

    /** The productId, allocatedTickets and hasImage of the given model are ignored. */
    public Product createProduct(Product product) {
        int productId = jooq.insertInto(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, product.name())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, product.description())
                .set(PRODUCTS.PRODUCT_PRICE, product.price())
                .set(PRODUCTS.PRODUCT_LOCATION, product.location())
                .set(PRODUCTS.PRODUCT_STARTS_AT, product.startsAt())
                .set(PRODUCTS.PRODUCT_MAX_TICKETS, product.maxTickets())
                .returning(PRODUCTS.PRODUCT_ID)
                .fetchOne(PRODUCTS.PRODUCT_ID);

        return getProductById(productId);
    }

    /**
     * The allocatedTickets and hasImage of the given model are ignored.
     *
     * @return the updated product, or null if no product has that id.
     */
    public Product updateProduct(Product product) {
        int updated = jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_NAME, product.name())
                .set(PRODUCTS.PRODUCT_DESCRIPTION, product.description())
                .set(PRODUCTS.PRODUCT_PRICE, product.price())
                .set(PRODUCTS.PRODUCT_LOCATION, product.location())
                .set(PRODUCTS.PRODUCT_STARTS_AT, product.startsAt())
                .set(PRODUCTS.PRODUCT_MAX_TICKETS, product.maxTickets())
                .where(PRODUCTS.PRODUCT_ID.eq(product.productId()))
                .execute();

        return updated > 0 ? getProductById(product.productId()) : null;
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

    /** Gives the tickets of a cancelled order back, so other customers can buy them. */
    public void releaseTickets(int productId, int quantity) {
        jooq.update(PRODUCTS)
                .set(PRODUCTS.PRODUCT_ALLOCATED_TICKETS, PRODUCTS.PRODUCT_ALLOCATED_TICKETS.minus(quantity))
                .where(PRODUCTS.PRODUCT_ID.eq(productId))
                .execute();
    }

    /** @return the picture, or null if the product has none. */
    public ProductImage getImage(int productId) {
        return jooq.select(PRODUCT_IMAGES.PRODUCT_IMAGE_CONTENT_TYPE, PRODUCT_IMAGES.PRODUCT_IMAGE_DATA)
                .from(PRODUCT_IMAGES)
                .where(PRODUCT_IMAGES.PRODUCT_IMAGE_PRODUCT_ID.eq(productId))
                .fetchOne(record -> new ProductImage(record.value1(), record.value2()));
    }

    /** Stores the picture, replacing the previous one. */
    public void saveImage(int productId, ProductImage image) {
        jooq.insertInto(PRODUCT_IMAGES)
                .set(PRODUCT_IMAGES.PRODUCT_IMAGE_PRODUCT_ID, productId)
                .set(PRODUCT_IMAGES.PRODUCT_IMAGE_CONTENT_TYPE, image.contentType())
                .set(PRODUCT_IMAGES.PRODUCT_IMAGE_DATA, image.data())
                .onConflict(PRODUCT_IMAGES.PRODUCT_IMAGE_PRODUCT_ID)
                .doUpdate()
                .set(PRODUCT_IMAGES.PRODUCT_IMAGE_CONTENT_TYPE, image.contentType())
                .set(PRODUCT_IMAGES.PRODUCT_IMAGE_DATA, image.data())
                .execute();
    }

    private SelectJoinStep<Record> selectProducts() {
        return jooq.select(PRODUCTS.fields())
                .select(HAS_IMAGE)
                .from(PRODUCTS);
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
                record.get(PRODUCTS.PRODUCT_ALLOCATED_TICKETS),
                record.get(HAS_IMAGE)
        );
    }
}
