# Frontend guide

Everything the website needs to know about this backend: addresses, logins, every request
with its fields and rules, the answers, and the pages the backend expects to exist. How the
backend works inside is in [documentation.md](documentation.md), how it is configured and
deployed in [README.md](README.md).

## 1. Setup

| | Development | Production (example) |
| --- | --- | --- |
| Backend (`API` below) | `http://localhost:8080` | `https://tickets.example.com/api` (= `TICKET_SSO_BACKEND_URL`) |
| Website | `http://localhost:3000` | `https://tickets.example.com` (= `TICKET_FRONTEND_URL`) |

- **The website must run exactly at the configured website address.** Only that origin is
  allowed by CORS, the backend redirects there after a login, and every QR code links there.
- **Send every request with `credentials: "include"`.** All logins are http-only cookies; the
  website never sees or stores a token.
- **Production: HTTPS, and website and backend on the same site** (same domain, e.g. the
  backend under `/api` of the website's domain). Otherwise browsers do not send the cookies.
- The backend itself has no `/api` prefix; all paths below are relative to `API`.
- The backend's API description: `{API}/q/openapi`, in development also the Swagger UI at
  `http://localhost:8080/q/swagger-ui`.

A small helper keeps this in one place:

```js
export const API = import.meta.env.VITE_API_URL; // e.g. "http://localhost:8080"

export async function api(path, { method = "GET", json, body, headers = {} } = {}) {
  return fetch(API + path, {
    method,
    credentials: "include",
    headers: json === undefined ? headers : { "Content-Type": "application/json", ...headers },
    body: json === undefined ? body : JSON.stringify(json),
  });
}
```

## 2. Data formats

| What | Format |
| --- | --- |
| Field names | camelCase, exactly as listed in this guide. Unknown fields in requests are ignored; a missing number counts as `0`, so always send every field. |
| Dates and times | Local time of the school (Europe/Berlin) **without time zone**, e.g. `"2027-06-26T18:00:00"`. When sending, seconds may be left out (`"2027-06-26T18:00"`, which is what `<input type="datetime-local">` gives). Answers may contain fractions of a second (`"2026-09-13T22:00:22.495636"`). Show them with `Intl.DateTimeFormat("de-DE", ...)`. |
| Money | JSON number in euros with at most 2 decimals, e.g. `45.00`. Show with `Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" })`. Order totals come from the backend (`total`). |
| Enums | Uppercase strings, e.g. `"PENDING"` |

## 3. Errors

| Status | Body | When |
| --- | --- | --- |
| 400 | `{ "title": "Constraint Violation", "status": 400, "violations": [ { "field": "createProduct.request.price", "message": "price must have at most 2 decimal places" } ] }` | A field breaks a rule from the tables below. The field name is the part after the last dot. |
| 400 | `{ "status": 400, "message": "The current password is not correct" }` | Wrong current password when changing the admin account |
| 401 | `{ "status": 401, "message": "You have to be logged in" }` on customer endpoints, empty on admin endpoints | Not logged in, or the session expired |
| 403 | empty | Logged in, but not allowed: door staff account, or admin setup not done |
| 404 | `{ "status": 404, "message": "..." }` or empty | Unknown id, unknown path or unknown enum value |
| 409 | `{ "status": 409, "message": "..." }` | A business rule, e.g. sold out, already paid, can no longer be cancelled |
| 413 / 415 | empty | Picture too big (about 10 MB) / not PNG, JPEG or WebP |
| 500 | not to rely on | Something failed on the server, e.g. the email of an order or payment could not be sent (see [10](#10-emails-the-customer-receives)). Nothing was changed; show a general error and let the user try again later. |

The messages are English and meant for developers. The website shows its own German texts
(formal "Sie", like the emails) depending on status and field.

## 4. Pages the backend relies on

| Path on the website | Why |
| --- | --- |
| `/tickets/{code}` | Every QR code in the ticket emails links here, see [7](#7-ticket-page-ticketscode) |
| `/?login=failed` | The backend sends the browser here when a customer login failed or was cancelled |
| the `returnTo` path of a login | After a successful customer login the browser comes back here, e.g. `/cart` |

## 5. Customer login (IServ, Moodle)

1. **Buttons:** `GET /auth/providers` returns the configured providers, e.g.
   `["ISERV", "MOODLE"]` (possibly only one, or none). Show one button per entry,
   e.g. "Mit IServ anmelden", "Mit Moodle anmelden".
2. **Start:** the button navigates the whole page, it must not use fetch:

   ```js
   const returnTo = "/cart";
   window.location.href = `${API}/auth/${provider.toLowerCase()}/login?returnTo=${encodeURIComponent(returnTo)}`;
   ```

   `returnTo` must be a path of the website starting with `/` (letters, digits and
   `- . _ ~ / ? = & %`); anything else becomes `/`. The page is left, so keep the cart in
   `localStorage`.
3. **Back on the website:** the customer is logged in. `GET /auth/me` returns
   `{ "firstName": "Max", "lastName": "Mustermann", "email": "max@schule.de", "provider": "ISERV" }`,
   or 401 if nobody is logged in.
4. **Failed or cancelled:** the browser lands on `/?login=failed`. Show e.g. "Die Anmeldung
   ist fehlgeschlagen oder wurde abgebrochen. Bitte versuchen Sie es erneut."
5. **Logout:** `POST /auth/logout` answers 204. The customer stays logged in at IServ/Moodle,
   so the next login may not ask for a password.

A customer session lasts 7 days. Any customer request can answer 401 when it has expired;
then show the login buttons again.

**Development without IServ/Moodle:** `GET /auth/dev/login?email=kunde@example.com&firstName=Test&lastName=Kunde`
(open it in the browser or fetch it with credentials) answers 204 and logs in a test customer.
Only works while the backend runs with `./mvnw quarkus:dev`.

## 6. Shop

### Ticket types

`GET /products` returns all ticket types, sorted by start time. `GET /products/{productId}`
returns one, or 404.

| Field | Type | Notes |
| --- | --- | --- |
| `productId` | number | |
| `name` | string | |
| `description` | string or `null` | Plain text |
| `price` | number | Price per ticket in euros |
| `location` | string | |
| `startsAt` | date-time | Start of the event |
| `maxTickets` | number | |
| `availableTickets` | number | Tickets left; `0` = sold out. Reserved but unpaid tickets count as taken. |
| `hasImage` | boolean | If `true`: `<img src="{API}/products/{productId}/image">` |

The picture endpoint answers 404 if there is none.

### Cart and placing an order

The cart lives only in the website, e.g. in `localStorage`, as product ids and quantities.
Prices are never sent; the backend takes them from the ticket types.

`POST /orders` (customer must be logged in):

```json
{ "items": [ { "productId": 1, "quantity": 3 }, { "productId": 2, "quantity": 2 } ] }
```

| Field | Rule |
| --- | --- |
| `items` | At least one entry |
| `items[].productId` | ≥ 1, an existing ticket type |
| `items[].quantity` | ≥ 1. The same product may appear twice; the quantities are added up. |

| Answer | Meaning | Website |
| --- | --- | --- |
| 201 + order | Tickets are reserved, payment email is sent | Show the confirmation (below), empty the cart |
| 401 | Not logged in | Start the login with `returnTo=/cart` |
| 404 | A ticket type no longer exists | Reload ticket types, fix the cart |
| 409 | e.g. `Only 2 tickets left for "Abiball"` | Reload ticket types, let the customer adjust the cart |
| 500 | e.g. the payment email could not be sent | Nothing was ordered; keep the cart and ask the customer to try again later |

If any item fails, nothing is reserved at all. An order only exists together with its payment
email: if the email cannot be sent, the order is not placed either.

**Confirmation page:** the order number `orderId` (it is also the bank transfer reference
"Bestellung {orderId}"), the `total`, and the payment deadline, the date of `paymentDueAt`
("bitte bis zum 20.09.2026 überweisen"). Mention that the bank details were sent by email;
the API does not return them.

### The order object

Returned by all order endpoints.

| Field | Type | Notes |
| --- | --- | --- |
| `orderId` | number | Also the bank transfer reference |
| `customerFirstName`, `customerLastName`, `customerEmail` | string | |
| `status` | `"PENDING"`, `"PAID"`, `"CANCELLED"` | See the customer and admin views below |
| `items` | array of `{ productId, productName, quantity, unitPrice }` | One entry per ticket type |
| `total` | number | Sum of all tickets |
| `createdAt` | date-time | When the order was placed |
| `paymentDueAt` | date-time | Always 23:59:59 of the last payment day |
| `paidAt` | date-time or `null` | When the admin confirmed the payment |

Ticket codes and QR codes are never part of an API answer; customers only get them by email
after the payment.

### My orders

`GET /orders/mine` returns the logged-in customer's orders, newest first (401 if not logged in).

The `status` here is the **customer's view**: an unpaid order is shown as `CANCELLED` as soon
as `paymentDueAt` has passed, although the admin still has one more day to confirm a late
transfer, so it can still turn into `PAID`.

| status | Suggested label |
| --- | --- |
| `PENDING` | "Zahlung ausstehend bis {paymentDueAt}" |
| `PAID` | "Bezahlt am {paidAt}, Tickets per E-Mail verschickt" |
| `CANCELLED` | "Storniert" |

**Cancelling:** show a button only for `PENDING` orders, with a confirmation dialog. The
whole order is cancelled.

`PATCH /orders/{orderId}/cancellation` (no body):

| Answer | Meaning |
| --- | --- |
| 200 + order | Cancelled, the tickets are free again |
| 404 | Unknown order, or not the customer's own |
| 409 | Can no longer be cancelled (paid, already cancelled, or past its due date); reload the list |

## 7. Ticket page `/tickets/{code}`

Every QR code opens this page. Who opens it decides what happens:

- **Door staff** (logged in with the door staff account, or an admin with the setup done):
  the page checks the guest in, which uses the ticket up if it is valid right now.
- **Everyone else**, e.g. a customer looking at their own ticket: the page only shows the
  state; the ticket is never used up.

```js
const me = await api("/admin/me");
const account = me.ok ? await me.json() : null;
const isDoorStaff = account !== null && (account.role === "DOOR_STAFF" || !account.setupRequired);

const response = isDoorStaff
  ? await api(`/tickets/${encodeURIComponent(code)}/check-in`, { method: "POST" })
  : await api(`/tickets/${encodeURIComponent(code)}`);
const check = await response.json();
```

Both answer 200. The check-in can also answer 401 (door login expired: show the login form)
or 403 (not allowed).

| Field | Type | Notes |
| --- | --- | --- |
| `result` | see below | |
| `productName`, `location` | string or `null` | `null` if the code belongs to no ticket |
| `startsAt` | date-time or `null` | Start of the event |
| `entryFrom` | date-time or `null` | From when the entrance accepts the ticket |
| `usedAt` | date-time or `null` | When the ticket was used |

| result | Meaning | Suggested display |
| --- | --- | --- |
| `ADMITTED` | Door check only: the ticket was valid and is now used up | Big green "Einlass OK" |
| `VALID` | Paid, unused, the entrance is open | "Ticket gültig" with event name |
| `NOT_YET_OPEN` | Paid and unused, but entry opens later | "Einlass ab {entryFrom}", event name, start time and location |
| `ALREADY_USED` | Somebody already entered with it | Red "Bereits entwertet am {usedAt}" |
| `INVALID` | Unknown code (`productName` is `null`), or the order is not paid | Red "Ticket ungültig" |

Reloading the page after `ADMITTED` shows `ALREADY_USED`; the time in `usedAt` makes clear
that it was this scan.

Door staff log in on the admin login page (section 8) with the door staff account. The QR
link opens in the phone's default browser, so they must log in in that browser. Faster for
many guests is a scanner view for door staff: read the QR code with the camera (e.g. a
JavaScript QR library), take the part after `/tickets/` from the link and call the check-in
directly. A door staff login expires after 30 minutes without requests.

## 8. Admin and door staff login

The login form sends form fields, not JSON:

```js
const response = await api("/admin/login", {
  method: "POST",
  body: new URLSearchParams({ username, password }),
});
// 200 = logged in, 401 = wrong username or password
```

Then `GET /admin/me`:

```json
{ "username": "admin", "role": "ADMIN", "email": null, "setupRequired": true }
```

| Account | Website shows |
| --- | --- |
| `role: "DOOR_STAFF"` | Only the door view (ticket page / scanner). Everything else answers 403. |
| `role: "ADMIN"`, `setupRequired: true` | Only the setup form below. Everything else answers 403. |
| `role: "ADMIN"`, `setupRequired: false` | The admin panel |

Logins of a fresh installation: `admin` / `admin` (must do the setup first) and
`einlass` / `einlass` (door staff). The admin login and the customer login are separate
cookies; one browser can have both.

A login expires after 30 minutes without requests; then admin requests answer 401 and the
website shows the login form again. `POST /admin/logout` answers 204.

### Setup form and changing the own account

`PUT /admin/account` (admin, also before the setup):

```json
{ "currentPassword": "admin", "newPassword": "Sommerfest-2027", "email": "admin@schule.de" }
```

| Field | Rule |
| --- | --- |
| `currentPassword` | Required; on the first setup `admin` |
| `newPassword` | 8 to 72 characters (may be the current one when only the email changes) |
| `email` | Required, a valid address, at most 255 characters |

Answers 200 with the account (`setupRequired: false`); 400 for a wrong current password or
an invalid field. The admin panel is open from the next request on, without a new login.

### Door staff password

`PUT /admin/door-staff-password` (admin): `{ "password": "Einlass-2027" }`, 8 to 72
characters, answers 204. Phones that are already logged in stay logged in until their login
expires.

## 9. Admin panel

### Ticket types

Create with `POST /products` (201 + ticket type), update with `PUT /products/{productId}`
(200 + ticket type, 404 if unknown). Both take the same body, and an update replaces all
fields, so always send the whole form. Ticket types cannot be deleted.

```json
{ "name": "Abiball 2027", "description": "Dinner und Party", "price": 45.00,
  "location": "Stadthalle", "startsAt": "2027-06-26T18:00", "maxTickets": 400 }
```

| Field | Type | Rule |
| --- | --- | --- |
| `name` | string | Required, at most 255 characters |
| `description` | string | Optional (`null` or left out) |
| `price` | number | Required, ≥ 0, at most 2 decimals |
| `location` | string | Required, at most 255 characters |
| `startsAt` | date-time | Required, local time |
| `maxTickets` | number | ≥ 0. May be lowered below the tickets already sold; then `availableTickets` is 0. |

**Picture:** uploaded separately, after the ticket type exists, with the file itself as body:

```js
await api(`/products/${productId}/image`, {
  method: "PUT",
  headers: { "Content-Type": file.type },
  body: file, // from <input type="file" accept="image/png,image/jpeg,image/webp">
});
```

| Answer | Meaning |
| --- | --- |
| 204 | Saved; replaces an earlier picture |
| 404 | Unknown ticket type |
| 413 | Bigger than about 10 MB |
| 415 | Not PNG, JPEG or WebP (SVG is refused on purpose) |

Suggested form flow: send the form, then upload the picture with the returned `productId`.
If the upload fails, the ticket type exists without picture; show the error and offer to
upload again. After replacing a picture, add e.g. `?v=${Date.now()}` to the `<img>` URL so
the browser loads the new one.

### Orders

`GET /orders?search=...&status=...` returns orders newest first. Both parameters are
optional; encode them with `encodeURIComponent`.

| Parameter | Meaning |
| --- | --- |
| `search` | The order number exactly, or any part of the customer's full name ("Max Muster") or email, ignoring case |
| `status` | `PENDING`, `PAID` or `CANCELLED`, uppercase (anything else answers 404) |

Here `status` is the **stored** status: a `PENDING` order whose `paymentDueAt` has passed is
in the admin's extra day. Mark it, e.g. "überfällig, noch bestätigbar bis {paymentDueAt + 1 Tag}".

`GET /orders/{orderId}` returns one order.

**Confirm payment:** `PATCH /orders/{orderId}/payment` (no body). Ask for confirmation first:
it emails the tickets and cannot be undone.

| Answer | Meaning |
| --- | --- |
| 200 + order | `PAID`, the tickets were emailed |
| 409 | Already paid; or the order was cancelled and not enough tickets are left to confirm it; or it was changed at the same moment (reload) |
| 500 | e.g. the ticket email could not be sent. The order stays unpaid; try again later. |

A `CANCELLED` order can still be confirmed as long as enough tickets are left, e.g. for a
transfer that arrived late.

### Settings

`GET /settings` and `PUT /settings`, always with both fields:

```json
{ "paymentDays": 7, "entryMinutesBeforeStart": 60 }
```

| Field | Rule | Effect |
| --- | --- | --- |
| `paymentDays` | 1 to 365 | Days a customer has to pay. Applies to orders placed afterwards. |
| `entryMinutesBeforeStart` | 0 to 1440 | How many minutes before an event's start the entrance accepts its tickets. Applies immediately. |

## 10. Emails the customer receives

The website does not send emails and configures nothing about them; sender, mail server and
support address are backend settings (see [README.md](README.md#emails)). Its texts should
match the emails, though. All emails are German, formal "Sie", and come from a noreply
address. Each names the support address (`SUPPORT_EMAIL` in the backend) and uses it as
Reply-To, so customers who press "Antworten" reach support. Show the same support address on
the website, e.g. in the footer or FAQ; the API does not return it.

| When | Subject | Content |
| --- | --- | --- |
| Order placed | "Bestellung {orderId}: Zahlungsinformationen" | Amount, account holder, IBAN, reference "Bestellung {orderId}", due date |
| Payment confirmed | "Bestellung {orderId}: Ihre Tickets" | One PNG QR code per ticket, linking to `{website}/tickets/{code}` |
| Not paid in time | "Bestellung {orderId}: Reservierung storniert" | The reservation was cancelled; contact support if already paid |

The payment and ticket emails are part of their action: if one cannot be sent, placing the
order or confirming the payment fails with 500 and nothing changes. If the cancellation email
fails, the order is cancelled anyway.

**During development** the backend usually does not send real emails (`./mvnw quarkus:dev`,
or `QUARKUS_MAILER_MOCK=true` with docker compose). Their texts are then written to the
backend log (`docker compose logs -f backend`), but not the QR code attachments. To test the
ticket page, take a ticket code from the database and open `http://localhost:3000/tickets/<code>`:

```
docker compose exec db psql -U app -d app -c "SELECT ticket_code, ticket_order_id FROM tickets ORDER BY ticket_id DESC LIMIT 5"
```

(`app` are the default `POSTGRES_USER` and `POSTGRES_DB`.) Only tickets of paid orders are
valid, so confirm the payment in the admin panel first.

## 11. Checklist

- [ ] `API` configured; the website runs exactly at `TICKET_FRONTEND_URL` of the backend
- [ ] Every request with `credentials: "include"`
- [ ] Production: HTTPS, website and backend on the same site
- [ ] Pages `/tickets/{code}` and `/?login=failed` exist
- [ ] Customer login by page navigation with `returnTo`; cart kept in `localStorage`
- [ ] Admin login with form fields; setup form when `setupRequired`; door view for `DOOR_STAFF`
- [ ] Dates sent and shown as local time without time zone; money formatted as EUR
- [ ] German texts with "Sie"; support address same as in the backend
- [ ] Placing an order and confirming a payment handle 500 (nothing changed, try again later)
