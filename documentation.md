# How the ticket backend works

## Layering

Client JSON -> Controller (DTO) -> Service (DTO <-> Model, business rules) ->
Repository (Model, SQL via jOOQ) -> Database, and back the same way. Controllers never
see a model, repositories never see a DTO; the conversion happens in `com.example.mapper`.

## Flow

1. The website lists all ticket types with price, picture and tickets left
   (`GET /products`) and shows one in detail (`GET /products/{productId}`).
2. The customer logs in via the SSO and places an order (`POST /orders`). One order can
   mix ticket types, e.g. 3 x ticket A and 2 x ticket B. The tickets are allocated right
   away, the order is `PENDING` and the customer gets an email with the bank transfer details.
3. An admin finds the order in the order list and confirms the transfer
   (`PATCH /orders/{orderId}/payment`). The order becomes `PAID` and the customer gets an
   email with one QR code per ticket.

## Payment deadline

"Payment days" is a setting in the admin panel. With 7 payment days:

| When | What happens |
| --- | --- |
| 01.09 10:00 | The customer orders. Due date: 08.09 23:59:59. |
| until 08.09 23:59:59 | The customer sees the order as `PENDING` and can still cancel it. |
| 09.09 | The customer sees the order as `CANCELLED`. It is still `PENDING` for the admin, who has this extra day to confirm a late transfer; the tickets stay allocated. |
| from 10.09 00:00 | A job that runs every minute sets the order to `CANCELLED`, releases the tickets and emails the customer. |

Even a cancelled order can still be confirmed by the admin, as long as enough tickets are
left: they are allocated again, otherwise the answer is 409. A changed number of payment
days applies to orders placed afterwards; existing orders keep the due date their
customers were already emailed.

## Customers

- **Order history:** `GET /orders/mine` lists every order of the logged-in customer, newest
  first, with order date (`createdAt`), due date, payment date (`paidAt`), items and total.
  The status is the customer's view described above.
- **Cancelling:** `PATCH /orders/{orderId}/cancellation` cancels the whole order while it
  is unpaid and before its due date. The tickets are released right away. Someone else's
  order answers 404.

## Admin panel

- **Ticket types:** the form sends name, description, price, location, start time and
  maximum number of tickets to `POST /products`. The picture is uploaded afterwards to
  `PUT /products/{productId}/image`, with the image file itself as request body (PNG, JPEG
  or WebP; SVG is refused because it can contain scripts). The same endpoint replaces a
  picture. `hasImage` in the product tells the website whether
  `GET /products/{productId}/image` has one.
- **Order list:** `GET /orders?search=...&status=...`, newest first. `search` matches the
  order number (the reference of the bank transfer), or any part of the customer's full
  name or email, ignoring case. `status` is `PENDING`, `PAID` or `CANCELLED` as stored, so
  an order in its extra day is still `PENDING` here. Both are optional.
- **Settings:** `GET /settings` and `PUT /settings` with `paymentDays` (1 to 365).

## Logins

- **Customers** log in via the SSO, which provides email, first name and last name. On
  their first request they are stored in `users`, identified by the SSO subject; email and
  names are refreshed on every later one.
- **Admins** do not come from the SSO. They log in with username and password:
  `POST /admin/login` with the form fields `username` and `password`
  (`application/x-www-form-urlencoded`) answers 200 and sets an encrypted, http-only
  cookie, or 401. The website has to send its requests with credentials
  (`fetch(url, { credentials: "include" })`). `GET /admin/me` tells whether the admin is
  still logged in, `POST /admin/logout` ends the session. Passwords are stored as bcrypt hashes.

Admin-only endpoints answer 401 without login.

### First admin login

A fresh installation creates the admin account `admin` with the password `admin` on
startup (whenever no admin exists at all).

1. The admin logs in with `admin` / `admin`.
2. `GET /admin/me` answers `"setupRequired": true`. Until the setup is done, every admin
   panel endpoint answers 403; only `/admin/me`, `/admin/account` and `/admin/logout` work.
3. The admin panel shows a form and sends it to `PUT /admin/account`:
   `{ "currentPassword": "admin", "newPassword": "...", "email": "..." }`. The new password
   needs 8 to 72 characters; a wrong current password answers 400.
4. From the next request on, the whole admin panel is open, without logging in again.

The same endpoint changes password and email address later. Forgotten password: delete
the admin from the `admins` table and restart, then the default account is created again.

**Not done yet:** until the SSO is connected, `security/CurrentUser` reads the customer
from the headers `X-User-Subject`, `X-User-Email`, `X-User-First-Name` and
`X-User-Last-Name`. Anyone can send these, so this has to be replaced before going live.

## Tables

- **users**: customers from the SSO (subject, email, first and last name).
- **admins**: username, bcrypt password hash and contact email address. The email is
  NULL until the admin finished the first-login setup.
- **products**: a ticket type: name, description, price, location, start time and the
  maximum number of tickets. `allocated_tickets` counts the tickets of pending and paid
  orders; it is raised with a single conditional UPDATE, so the last ticket cannot be
  sold twice, and lowered again when an order is cancelled.
- **product_images**: the picture of a product. A separate table, so listing products does
  not load every picture.
- **orders**: who ordered, status (`PENDING`, `PAID`, `CANCELLED`), when the payment is due
  and when it arrived. The total is the sum of its tickets. Every status change checks the
  previous status in the same UPDATE, so paying and cancelling cannot overlap.
- **tickets**: one row per person, so 3 x A and 2 x B are 5 rows; the API groups them back
  into items. Each row stores its product, the price at order time and a random code
  (144 bits, Base64url) that cannot be guessed.
- **settings**: exactly one row with the settings of the admin panel.

A ticket's QR code holds the link `{ticket.frontend-url}/tickets/{code}`, so scanning it
opens the website.

## Endpoints

| Method | Path | Who | Purpose |
| --- | --- | --- | --- |
| GET | /products | everyone | all ticket types |
| GET | /products/{productId} | everyone | one ticket type |
| GET | /products/{productId}/image | everyone | its picture (404 if none) |
| POST | /products | admin | create (201) |
| PUT | /products/{productId} | admin | update |
| PUT | /products/{productId}/image | admin | upload or replace the picture (204; 415 for other file types) |
| POST | /orders | customer | place an order (201; 409 if too few tickets left) |
| GET | /orders/mine | customer | own order history |
| PATCH | /orders/{orderId}/cancellation | customer | cancel an own unpaid order (409 if paid, cancelled or overdue) |
| GET | /orders?search=&status= | admin | order list, newest first |
| GET | /orders/{orderId} | admin | one order |
| PATCH | /orders/{orderId}/payment | admin | confirm payment and send tickets (409 if already paid or too few tickets left) |
| GET | /settings | admin | the settings |
| PUT | /settings | admin | change the settings |
| POST | /admin/login | everyone | admin login (form fields, see above) |
| GET | /admin/me | admin, also before setup | the logged-in admin, incl. `setupRequired` |
| PUT | /admin/account | admin, also before setup | set a new password and the email address |
| POST | /admin/logout | admin, also before setup | log out (204) |

Errors come back as `ErrorResponse`: 400 invalid input (e.g. wrong current password),
401 not logged in, 403 admin setup not done yet, 404 unknown id, 409 business rule
violated. Failed bean validation also answers 400.

Example bodies:

```json
POST /products
{ "name": "Abiball 2027", "description": "Dinner und Party", "price": 45.00,
  "location": "Stadthalle", "startsAt": "2027-06-26T18:00:00", "maxTickets": 400 }

POST /orders
{ "items": [ { "productId": 1, "quantity": 3 }, { "productId": 2, "quantity": 2 } ] }

PUT /settings
{ "paymentDays": 7 }
```

## Configuration

Customer emails are sent from a noreply address (`MAIL_FROM`). Every email names the
support address (`SUPPORT_EMAIL`) and uses it as Reply-To. That address only forwards to
the admin's own mailbox (set up at the mail provider), so customers never see it; the admin
may answer from their own address.

The repository is public, so nothing secret goes into a committed file. Secret or
installation-specific values live in `.env` (see `.env.example`) and are referenced from
`application.properties` as `${...}`; Quarkus and docker compose both read it. The website
address is set directly in `application.properties` as `ticket.frontend-url`.
