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

## Users, tickets and QR codes (V0003)

### Models

- User (int userId, String firstName, String lastName, String email, UserRole role,
  AuthProvider authProvider, String externalId, String passwordHash,
  LocalDateTime createdAt) - `passwordHash` never leaves the service layer
- UserRole (USER, CREATOR, ADMIN)
- AuthProvider (LOCAL, ISERV, MOODLE) - LOCAL is the password login kept for admins,
  the others are the school's identity providers. `externalId` is the provider's
  subject and is null for LOCAL accounts; `passwordHash` is the other way round
- Ticket (int ticketId, String code, int orderId, int productId, String productName,
  TicketStatus status, LocalDateTime issuedAt, LocalDateTime usedAt)
- TicketStatus (VALID, USED, CANCELLED)

Product gained `validFrom` / `validUntil`: the entry window of that ticket type.
Null on either side means no limit in that direction, so an 18:00 ticket and a
22:00 late entry ticket are two products with different windows.

### QR codes

- `TicketCodeGenerator` draws a 24 character code from SecureRandom over a 32
  character alphabet (no I, O, 0, 1). 120 bits, so a code cannot be guessed from
  other codes; `ticket_code` is UNIQUE as the backstop.
- `QrCodeGenerator` renders a code as a PNG (zxing, error correction Q).
- `TicketValidator` decides admission and touches no database, so the awkward
  cases are testable on their own. It does not consume the ticket - that is the
  conditional UPDATE in `TicketService.checkIn`, which is what stops two doors
  scanning the same code at the same moment.

Rejection reasons: ALREADY_USED, TICKET_CANCELLED, ORDER_NOT_PAID, NOT_YET_VALID,
EXPIRED.

### Flow

Order paid -> one ticket per admitted person (quantity 3 gives 3 codes) ->
QR sent out -> scanned at the door -> ticket becomes USED. Cancelling an order
invalidates its outstanding tickets; already used ones keep their status.

### Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | /users | list users |
| GET | /users/{userId} | one user |
| POST | /users | create (201; 409 if the email is taken) |
| GET | /tickets/order/{orderId} | the tickets of one order |
| GET | /tickets/{code} | look a code up without consuming it |
| GET | /tickets/{code}/qr | the QR image (image/png) |
| POST | /tickets/{code}/check-in | admit: validate and consume (409 with the reason) |
