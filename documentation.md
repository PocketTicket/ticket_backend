# How the ticket backend works

## Layering

Client JSON -> Controller (DTO) -> Service (DTO <-> Model, business rules) ->
Repository (Model, SQL via jOOQ) -> Database, and back the same way. Controllers never
see a model, repositories never see a DTO; the conversion happens in `com.example.mapper`.

## Flow

1. The website lists all ticket types with price and tickets left (`GET /products`)
   and shows one in detail (`GET /products/{productId}`).
2. The customer logs in via the SSO and places an order (`POST /orders`). One order can
   mix ticket types, e.g. 3 x ticket A and 2 x ticket B. The tickets are allocated right
   away, the order is `PENDING` and the customer gets an email with the bank transfer
   details. The payment is due after `ticket.payment-days`.
3. An admin logs in on the admin login form and confirms the transfer
   (`PATCH /orders/{orderId}/payment`). The order becomes `PAID` and the customer gets an
   email with one QR code per ticket.

## Logins

- **Customers** log in via the SSO, which provides email, first name and last name. On
  their first order they are stored in `users`, identified by the SSO subject; email and
  names are refreshed on every later order.
- **Admins** do not come from the SSO. They log in with username and password:
  `POST /admin/login` with the form fields `username` and `password`
  (`application/x-www-form-urlencoded`) answers 200 and sets an encrypted, http-only
  cookie, or 401. The website has to send its requests with credentials
  (`fetch(url, { credentials: "include" })`). `GET /admin/me` tells whether the admin is
  still logged in, `POST /admin/logout` ends the session. The admin from `.env`
  (`ADMIN_USERNAME`, `ADMIN_PASSWORD`) is created on startup; passwords are stored as
  bcrypt hashes.

Admin-only endpoints answer 401 without login.

**Not done yet:** until the SSO is connected, `security/CurrentUser` reads the customer
from the headers `X-User-Subject`, `X-User-Email`, `X-User-First-Name` and
`X-User-Last-Name`. Anyone can send these, so this has to be replaced before going live.

## Tables

- **users**: customers from the SSO (subject, email, first and last name).
- **admins**: username and bcrypt password hash.
- **products**: a ticket type: name, description, price, location, start time and the
  maximum number of tickets. `allocated_tickets` counts the tickets of pending and paid
  orders; it is raised with a single conditional UPDATE, so the last ticket cannot be sold twice.
- **orders**: who ordered, status (`PENDING`, `PAID`, `CANCELLED`), when the payment is due
  and when it arrived. The total is the sum of its tickets.
- **tickets**: one row per person, so 3 x A and 2 x B are 5 rows; the API groups them back
  into items. Each row stores its product, the price at order time and a random code
  (144 bits, Base64url) that cannot be guessed.

A ticket's QR code holds the link `{ticket.frontend-url}/tickets/{code}`, so scanning it
opens the website.

## Endpoints

| Method | Path | Who | Purpose |
| --- | --- | --- | --- |
| GET | /products | everyone | all ticket types |
| GET | /products/{productId} | everyone | one ticket type |
| POST | /products | admin | create (201) |
| PUT | /products/{productId} | admin | update |
| POST | /orders | customer | place an order (201; 409 if too few tickets left) |
| GET | /orders | admin | all orders |
| GET | /orders/{orderId} | admin | one order |
| PATCH | /orders/{orderId}/payment | admin | confirm payment and send tickets (409 if not pending) |
| POST | /admin/login | everyone | admin login (form fields, see above) |
| GET | /admin/me | admin | the logged-in admin |
| POST | /admin/logout | admin | log out (204) |

Errors come back as `ErrorResponse`: 401 not logged in, 404 unknown id, 409 business rule
violated. Failed bean validation answers 400.

Example bodies:

```json
POST /products
{ "name": "Abiball 2027", "description": "Dinner und Party", "price": 45.00,
  "location": "Stadthalle", "startsAt": "2027-06-26T18:00:00", "maxTickets": 400 }

POST /orders
{ "items": [ { "productId": 1, "quantity": 3 }, { "productId": 2, "quantity": 2 } ] }
```

## Configuration

The repository is public, so nothing secret goes into a committed file. Secret or
installation-specific values live in `.env` (see `.env.example`) and are referenced from
`application.properties` as `${...}`; Quarkus and docker compose both read it. Everything
else is set directly in `application.properties`: `ticket.payment-days` and
`ticket.frontend-url`.
