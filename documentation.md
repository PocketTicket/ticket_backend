# This documentation holds basic model information

## Layering

Client JSON -> Controller (DTO) -> Service (DTO <-> Model, business rules) ->
Repository (Model, SQL) -> Database, and back the same way. Controllers never see
a model, repositories never see a DTO, and the conversion happens in
`com.example.mapper`, so neither package has to import the other.

## Models (com.example.models)

- Order (int orderId, int userId, List&lt;OrderItem&gt; items, BigDecimal totalAmount,
  LocalDateTime orderDate, LocalDateTime paymentDueDate, LocalDateTime paymentDate,
  OrderStatus status)
- OrderItem (int orderItemId, int productId, String productName, int quantity,
  BigDecimal unitPrice) - `productName` is joined from products for display,
  `unitPrice` is the price snapshot taken when the order was placed
- OrderStatus (ORDERED, PAID, DELIVERED, REVOKED, CANCELLED)
- Product (int productId, String name, String description, BigDecimal price, int stock)

Money is `BigDecimal` everywhere to match the `DECIMAL(10,2)` columns.

## DTOs (com.example.dto)

Inbound:
- OrderRequest (userId, items) - no total; it is calculated from the products
- OrderItemRequest (productId, quantity) - no price; it is read from the product
- ProductCreateRequest / ProductUpdateRequest (name, description, price, stock)

Outbound:
- OrderResponse (orderId, userId, items, total, orderDate, paymentDueDate,
  paymentDate, status)
- OrderItemResponse (productId, productName, quantity, unitPrice, lineTotal)
- ProductResponse (productId, name, description, price, stock)
- ErrorResponse (status, message)

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | /products | list products |
| GET | /products/{productId} | one product |
| POST | /products | create (201) |
| PUT | /products/{productId} | replace |
| DELETE | /products/{productId} | delete (204; 409 if already ordered) |
| GET | /orders | all orders, admin |
| GET | /orders/user/{userId} | orders of one user |
| GET | /orders/{orderId} | one order |
| POST | /orders | place an order (201) |
| PATCH | /orders/{orderId}/payment | record the bank transfer, admin |
| PATCH | /orders/{orderId}/cancellation | cancel and release the tickets |

Errors come back as `ErrorResponse`: 404 unknown id, 409 business rule violated,
400 failed bean validation.
