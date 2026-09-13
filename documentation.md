# How the ticket backend works

## Layering

Client JSON -> Controller (DTO) -> Service (DTO <-> Model, business rules) ->
Repository (Model, SQL via jOOQ) -> Database, and back the same way. Controllers never
see a model, repositories never see a DTO; the conversion happens in `com.example.mapper`.
Only the enums of `com.example.models` (e.g. `OrderStatus`) are used by DTOs too.

## Flow

1. The website lists all ticket types with price, picture and tickets left
   (`GET /products`) and shows one in detail (`GET /products/{productId}`).
2. The customer logs in via the SSO and places an order (`POST /orders`). One order can
   mix ticket types, e.g. 3 x ticket A and 2 x ticket B. The tickets are allocated right
   away, the order is `PENDING` and the customer gets an email with the bank transfer details.
3. An admin finds the order in the order list and confirms the transfer
   (`PATCH /orders/{orderId}/payment`). The order becomes `PAID` and the customer gets an
   email with one QR code per ticket.
4. At the entrance, door staff scan the QR code and the ticket is used up (see Entrance).

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

## Entrance

A ticket's QR code opens the website at `/tickets/{code}`. The page first asks
`GET /admin/me` (with credentials):

- **Door staff** are logged in with the shared door staff account (or as an admin with the
  setup done). The page calls `POST /tickets/{code}/check-in`, which lets the guest in and
  uses the ticket up if it is valid right now.
- **Everyone else**, e.g. a customer checking their own ticket, gets `GET /tickets/{code}`.
  It shows the same state but never uses the ticket up, so a photographed or self-scanned
  QR code cannot invalidate a ticket.

Both answer 200 with `result` and, for a known code, `productName`, `location`,
`startsAt`, `entryFrom` and `usedAt`:

| result | meaning | the page shows |
| --- | --- | --- |
| `ADMITTED` | door check only: was valid, is now used up | let the guest in |
| `VALID` | paid, unused, entry is open | the ticket is valid |
| `NOT_YET_OPEN` | paid and unused, entry opens at `entryFrom` | start time and location of the event |
| `ALREADY_USED` | somebody entered with it at `usedAt` | the ticket was already used |
| `INVALID` | unknown code (all other fields null), or its order is not paid | the ticket is not valid |

Entry opens `entryMinutesBeforeStart` minutes (admin setting, default 0) before the event
starts. A ticket can be used exactly once: the UPDATE that uses it up only succeeds while it
is still unused, so two scanners at the same moment cannot both admit the same ticket.

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
- **Settings:** `GET /settings` and `PUT /settings` with `paymentDays` (1 to 365) and
  `entryMinutesBeforeStart` (0 to 1440; how long before an event's start the entrance opens).

## Logins

- **Customers** log in via IServ or Moodle (see Customer login below), which provide email,
  first name and last name. They are stored in `users` on their first login, identified by
  provider and subject; email and names are refreshed on every later login.
- **Admins** do not come from the SSO. They log in with username and password:
  `POST /admin/login` with the form fields `username` and `password`
  (`application/x-www-form-urlencoded`) answers 200 and sets an encrypted, http-only
  cookie, or 401. The website has to send its requests with credentials
  (`fetch(url, { credentials: "include" })`). `GET /admin/me` tells whether the admin is
  still logged in, `POST /admin/logout` ends the session. Passwords are stored as bcrypt hashes.

Admin-only endpoints answer 401 without login.

### First admin login

A fresh installation creates the admin account `admin` with the password `admin` on
startup (whenever no admin exists).

1. The admin logs in with `admin` / `admin`.
2. `GET /admin/me` answers `"setupRequired": true`. Until the setup is done, every admin
   panel endpoint answers 403; only `/admin/me`, `/admin/account` and `/admin/logout` work.
3. The admin panel shows a form and sends it to `PUT /admin/account`:
   `{ "currentPassword": "admin", "newPassword": "...", "email": "..." }`. The new password
   needs 8 to 72 characters; a wrong current password answers 400.
4. From the next request on, the whole admin panel is open, without logging in again.

The same endpoint changes password and email address later. Forgotten password: delete
the admin from the `admins` table and restart, then the default account is created again.

### Door staff

All door staff share one account, `einlass` with the password `einlass`, created on startup
whenever no door staff account exists. They log in on the same login form, on as many phones
at once as needed. The account can only check tickets in (`POST /tickets/{code}/check-in`),
see itself (`GET /admin/me`, `"role": "DOOR_STAFF"`) and log out; everything else answers 403,
including changing its own password. So its login may be known to every helper. The admin
can change the password with `PUT /admin/door-staff-password` `{ "password": "..." }`
(8 to 72 characters), e.g. before the event or if the login spread further than intended.
A login stays valid for 30 minutes without requests; every scan renews it.

### Customer login (IServ, Moodle)

The backend runs the OpenID Connect authorization code flow itself, without Keycloak or
another service in between:

1. The website shows one button per provider from `GET /auth/providers`
   (e.g. `["ISERV", "MOODLE"]`). A button is a normal link, not fetch, to
   `{backend}/auth/iserv/login?returnTo=/cart` (or `/auth/moodle/login`).
2. The backend redirects the browser to IServ or Moodle, where the customer logs in.
3. IServ or Moodle redirect back to `{backend}/auth/iserv/callback` with a one-time code. The
   backend trades it for an access token (client secret and PKCE), asks the provider for
   `sub`, `email`, `given_name` and `family_name`, stores the customer and sets the session
   cookie `ticket_session` (http-only, 7 days).
4. The browser ends up at `{frontend}{returnTo}`, or at `{frontend}/?login=failed` if the
   customer cancelled or something went wrong (the reason is in the backend log).

The website calls the API with `credentials: "include"`. `GET /auth/me` returns the
logged-in customer (401 if nobody is logged in), `POST /auth/logout` ends the session. The
cookies are only sent over HTTPS, so production needs HTTPS (localhost works without).

The code is in `security/SsoClient.java` (talking to IServ and Moodle),
`service/AuthService.java` (sessions) and `controller/AuthController.java` (redirects and
cookies). A provider that is not configured is not offered. How to register the backend at
IServ and Moodle, all settings, troubleshooting and the sources are in
[README.md](README.md#customer-login-sso).

**Local development:** without an IServ or Moodle, open `{backend}/auth/dev/login`
(optionally `?email=...&firstName=...&lastName=...`) to be logged in as a test customer.
This only works with `./mvnw quarkus:dev` and answers 404 otherwise.

## Tables

- **users**: customers from IServ or Moodle (provider, subject, email, first and last name).
  The subject is only unique within its provider.
- **user_sessions**: logged-in customers. Only the SHA-256 hash of the cookie token is stored,
  so the table alone cannot be used to take over a session; expired rows are removed on the
  next login.
- **admins**: the accounts of the login form: username, role (`ADMIN` or `DOOR_STAFF`),
  bcrypt password hash and contact email address. The email is NULL until an admin finished
  the first-login setup, and always NULL for the door staff account.
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
  (144 bits, Base64url) that cannot be guessed. `used_at` is set when the ticket is used
  at the entrance.
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
| GET | /tickets/{code} | everyone | state of a ticket, never uses it up |
| POST | /tickets/{code}/check-in | door staff, admin | door check, uses the ticket up if valid |
| GET | /settings | admin | the settings |
| PUT | /settings | admin | change the settings |
| GET | /auth/providers | everyone | the configured login providers |
| GET | /auth/{provider}/login?returnTo= | everyone | start the customer login (browser link, redirects) |
| GET | /auth/{provider}/callback | IServ/Moodle | end of the login, redirects to the website |
| GET | /auth/me | customer | the logged-in customer |
| POST | /auth/logout | customer | log out (204) |
| GET | /auth/dev/login | dev mode only | log in as a test customer (204) |
| POST | /admin/login | everyone | admin login (form fields, see above) |
| GET | /admin/me | door staff, admin (also before setup) | the logged-in account, incl. `role` and `setupRequired` |
| PUT | /admin/account | admin, also before setup | set a new password and the email address |
| PUT | /admin/door-staff-password | admin | set the password of the door staff account (204) |
| POST | /admin/logout | door staff, admin (also before setup) | log out (204) |

Errors come back as `ErrorResponse`: 400 invalid input (e.g. wrong current password),
401 not logged in, 403 not allowed (door staff account, or admin setup not done yet),
404 unknown id, 409 business rule
violated. Failed bean validation also answers 400.

Example bodies:

```json
POST /products
{ "name": "Abiball 2027", "description": "Dinner und Party", "price": 45.00,
  "location": "Stadthalle", "startsAt": "2027-06-26T18:00:00", "maxTickets": 400 }

POST /orders
{ "items": [ { "productId": 1, "quantity": 3 }, { "productId": 2, "quantity": 2 } ] }

PUT /settings
{ "paymentDays": 7, "entryMinutesBeforeStart": 60 }
```

## Configuration

Customer emails are sent from a noreply address (`MAIL_FROM`). Every email names the
support address (`SUPPORT_EMAIL`) and uses it as Reply-To. That address only forwards to
the admin's own mailbox (set up at the mail provider), so customers never see it; the admin
may answer from their own address.

The repository is public, so nothing secret goes into a committed file. Secret or
installation-specific values live in `.env` (see `.env.example`) and are referenced from
`application.properties` as `${...}`; Quarkus and docker compose both read it. All variables
are listed in [README.md](README.md#configuration).

Dates and times (event start, due dates, entry) are `LocalDateTime` without time zone, in the
school's local time; the backend has to run with the time zone Europe/Berlin.
