# ticket_backend

Backend of a ticketing system for (school) events, built with Quarkus, jOOQ, Flyway and
PostgreSQL.

| File | For |
| --- | --- |
| README.md (this file) | Running, configuring and deploying the backend, setting up IServ and Moodle |
| [documentation.md](documentation.md) | How the backend works: flows, rules, tables, endpoints |
| [frontend-guide.md](frontend-guide.md) | Everything the website needs: requests, fields, answers, logins, pages |

## Running

First copy `.env.example` to `.env` and fill in the values (see [Configuration](#configuration)).
`.env` holds everything secret and is never committed.

### Everything with docker compose

```
docker compose up -d
```

starts the database and the backend on <http://localhost:8080>. The first start builds the
backend image (a few minutes); after code changes run `docker compose up -d --build`.
`docker compose logs -f backend` shows the log, `docker compose down` stops everything
(the data stays in the volume `db`; `docker compose down -v` deletes it).

Without a real mail server, set `QUARKUS_MAILER_MOCK=true`: otherwise placing an order fails,
because its email cannot be sent. The emails are then only written to the log; the `MAIL_*`
values still have to be set, but placeholders are fine.

### Development

1. Start only the database: `docker compose up -d db`
2. Start the backend with live reload: `./mvnw quarkus:dev` (<http://localhost:8080>; stop the
   backend container first, it uses the same port)
3. Run the website on <http://localhost:3000>. That address is allowed by CORS and used for
   redirects and QR code links; change `TICKET_FRONTEND_URL` if the website runs elsewhere.

Every build applies the Flyway migrations to that database and generates the jOOQ classes
from it, so the database has to be running to build. The build connects with the
`jooq.codegen.jdbc.*` values in `pom.xml` (`app`/`app`/`app`). The Docker build does not need
it: it starts its own temporary database.

- Swagger UI: <http://localhost:8080/q/swagger-ui>
- Emails are not sent in dev mode but written to the log.
- Without an IServ or Moodle, <http://localhost:8080/auth/dev/login> logs you in as a test
  customer (dev mode only, answers 404 otherwise).
- Reset the local database: `./mvnw flyway:clean`, then start again.

Logins of a fresh installation:

| Login | Password | Can do |
| --- | --- | --- |
| `admin` | `admin` | Only set a personal password and an email address; then the whole admin panel |
| `einlass` | `einlass` | Only check tickets in at the entrance (shared by all door staff) |

## Configuration

The repository is public, so nothing secret goes into a committed file. Secret or
installation-specific values live in `.env` and are read by Quarkus and docker compose.
Quarkus turns an environment variable like `TICKET_FRONTEND_URL` into the property
`ticket.frontend-url`, so every property of `application.properties` can be overridden this
way ([Quarkus configuration reference](https://quarkus.io/guides/config-reference)).

| Variable | Needed | Example | Meaning |
| --- | --- | --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | always | `app` | Database; also used by docker compose. Local builds expect `app`/`app`/`app`. |
| `SESSION_ENCRYPTION_KEY` | always | output of `openssl rand -base64 32` | Encrypts the login cookie of admins and door staff. Changing it logs them out. |
| `BANK_RECIPIENT`, `BANK_IBAN` | always | `Förderverein Musterschule`, `DE89 3704 ...` | Shown in the payment email |
| `MAIL_FROM` | always | `tickets@noreply.example.com` | Sender of all emails, see [Emails](#emails) |
| `SUPPORT_EMAIL` | always | `support@example.com` | Named in every email and used as its Reply-To |
| `TICKET_SSO_ISERV_URL`, `TICKET_SSO_ISERV_CLIENT_ID`, `TICKET_SSO_ISERV_CLIENT_SECRET` | for IServ login | `https://mein-iserv.de` | See [Customer login](#customer-login-sso). Leave all three out to not offer IServ. |
| `TICKET_SSO_MOODLE_URL`, `TICKET_SSO_MOODLE_CLIENT_ID`, `TICKET_SSO_MOODLE_CLIENT_SECRET` | for Moodle login | `https://moodle.example.com` | The same for Moodle |
| `TICKET_FRONTEND_URL` | production | `https://tickets.example.com` | The website, without trailing slash. Allowed by CORS, target of redirects after login, start of every QR code link. Default `http://localhost:3000`. |
| `TICKET_SSO_BACKEND_URL` | production | `https://tickets.example.com/api` | This backend as the browser sees it, without trailing slash. The redirect URIs registered at IServ and Moodle start with it. Default `http://localhost:8080`. |
| `QUARKUS_DATASOURCE_JDBC_URL` | production | `jdbc:postgresql://db:5432/app` | Only if the database is not at `localhost:5432` |
| `MAIL_HOST`, `MAIL_PORT` | always | `smtp.example.com`, `587` | SMTP server of the sender address, see [Emails](#emails). Placeholders are fine while emails are only logged. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | always | `tickets@noreply.example.com`, ... | Login of the sender mailbox at that server (often an app password) |
| `MAIL_TLS`, `MAIL_START_TLS` | for port 465 | `false`, `REQUIRED` | Encryption. Port 587: `false` and `REQUIRED` (the defaults). Port 465: `true` and `DISABLED`. |
| `QUARKUS_MAILER_MOCK` | only without mail server | `true` | Emails are only written to the log. Never in production. |

Empty values are not allowed: leave out a line that is not needed instead of writing
`TICKET_SSO_MOODLE_URL=`.

Set in the admin panel, not in files: payment days, how long before an event the entrance
opens, the door staff password, and the admin's password and email address.

## Customer login (SSO)

Customers log in with the account of their school's IServ or Moodle. The backend talks to
them with OpenID Connect (authorization code flow with client secret and PKCE); no Keycloak
or other service is needed. How the login works step by step is described in
[documentation.md](documentation.md#customer-login-iserv-moodle), what the website has to do
in [frontend-guide.md](frontend-guide.md#5-customer-login-iserv-moodle).

Either provider can be used alone, or both at once. The website only shows the buttons of the
providers that are configured.

### 1. Decide the addresses

Before registering anything, fix where backend and website will be reachable, because the
redirect URIs must match exactly (scheme, host, path, no trailing slash):

| Setting | Development | Production example |
| --- | --- | --- |
| `TICKET_SSO_BACKEND_URL` | `http://localhost:8080` | `https://tickets.example.com/api` |
| IServ redirect URI | `http://localhost:8080/auth/iserv/callback` | `https://tickets.example.com/api/auth/iserv/callback` |
| Moodle redirect URI | `http://localhost:8080/auth/moodle/callback` | `https://tickets.example.com/api/auth/moodle/callback` |

Production needs HTTPS; see [Production](#production).

### 2. IServ

This is done by the school's IServ administrator in the IServ web interface under
**Administration → System → Single-Sign-On → Add** ([IServ: Single-Sign-On](https://doku.iserv.de/manage/system/sso/)):

1. **Name:** e.g. `Tickets`.
2. **Trusted:** yes, so users are not asked for consent on every login.
3. **Client ID** and **client secret:** note both for `.env`.
4. **Access:** optionally limit the login to certain groups or roles, e.g. only students and
   parents of the graduating year. Leave it empty to allow everyone.
5. **Grant types:** `Authorization Code`. Nothing else is needed.
6. **Scopes:** `openid`, `profile` and `email`. The backend needs the claims `sub`, `email`,
   `given_name` and `family_name` from them.
7. **Redirect URI:** the IServ redirect URI from step 1.
8. Users need the IServ right **"OAuth verwenden"**; without it IServ refuses their login.

Then add to `.env`:

```
TICKET_SSO_ISERV_URL=https://mein-iserv.de
TICKET_SSO_ISERV_CLIENT_ID=<client ID>
TICKET_SSO_ISERV_CLIENT_SECRET=<client secret>
```

`TICKET_SSO_ISERV_URL` is the address of the school's IServ without trailing slash. To check
it, open `https://mein-iserv.de/.well-known/openid-configuration`: the backend uses
`/iserv/auth/auth`, `/iserv/auth/public/token` and `/iserv/auth/userinfo` below that address,
which must match `authorization_endpoint`, `token_endpoint` and `userinfo_endpoint` there. If
an IServ ever uses other paths, they are in `security/SsoClient.java` (`endpoints`).

Not to be confused with the central "Login mit IServ" service, which IServ discontinued in
2024; this backend uses the SSO of the school's own IServ.

### 3. Moodle

Moodle cannot act as a login provider on its own. The Moodle administrator installs the
plugin **OAuth2 Server** (`local_oauth2`, needs Moodle 4.5 or newer):

1. Install the plugin: download it from the [Moodle plugins directory](https://moodle.org/plugins/local_oauth2)
   and install it under **Site administration → Plugins → Install plugins**, or unpack it into
   `local/oauth2` of the Moodle installation and open the notifications page
   ([Moodle: Installing plugins](https://docs.moodle.org/en/Installing_plugins)).
2. Add a client under **Site administration → Server → OAuth2 server → Manage OAuth clients**
   ([plugin README](https://github.com/enovation/moodle-local_oauth2)):
   - **Client identifier** and **client secret:** note both for `.env`
   - **Redirect URL:** the Moodle redirect URI from step 1
   - **Scopes**, if the form asks for them: `openid`, `profile`, `email`

Then add to `.env`:

```
TICKET_SSO_MOODLE_URL=https://moodle.example.com
TICKET_SSO_MOODLE_CLIENT_ID=<client identifier>
TICKET_SSO_MOODLE_CLIENT_SECRET=<client secret>
```

To check the address, open `https://moodle.example.com/local/oauth2/openid_configuration.php`:
the backend uses `/local/oauth2/login.php`, `/local/oauth2/token.php` and
`/local/oauth2/userinfo.php` below `TICKET_SSO_MOODLE_URL`.

### 4. Test the login

1. Restart the backend. `GET /auth/providers` must list the provider, e.g. `["ISERV"]`.
2. Open `{TICKET_SSO_BACKEND_URL}/auth/iserv/login` in the browser, log in at IServ.
3. The browser must end up on the website, and `GET /auth/me` must show your name.

### 5. Troubleshooting

When a login fails, the browser ends up at `{TICKET_FRONTEND_URL}/?login=failed` and the
backend log usually has a line `Login via ISERV failed: ...` with the reason.

| What you see | Cause | Fix |
| --- | --- | --- |
| IServ or Moodle show an error page instead of their login | The redirect URI sent by the backend is not registered, or the client ID is wrong | Register exactly `{TICKET_SSO_BACKEND_URL}/auth/iserv/callback`; check http/https, `/api` prefix, trailing slash |
| Log: `answered HTTP 401 ... invalid_client` | Client ID or secret in `.env` wrong | Copy both again |
| Log: `answered HTTP 400 ... invalid_grant` | Redirect URI differs between login and callback, or the code expired | Check `TICKET_SSO_BACKEND_URL`; try again |
| Log: `The SSO answer has no "email"` (or `given_name`, `family_name`) | The client is not allowed the scopes `email`/`profile` | Allow the scopes in IServ or Moodle |
| IServ refuses the user | User lacks the right "OAuth verwenden", or is not in an allowed group | Grant the right or adjust the access limit |
| Log: `Could not reach https://...` | The backend server cannot reach IServ or Moodle | Check DNS and firewall of the server |
| `?login=failed` without a log line | The browser did not send the login cookie back: not HTTPS in production, the login was started on another address than `TICKET_SSO_BACKEND_URL`, or it took longer than 10 minutes | Use HTTPS and the configured address |
| Login works, but `GET /auth/me` answers 401 | The website sends no cookies: not on the same site as the backend, or `fetch` without `credentials: "include"` | See [Production](#production) and the frontend guide |
| `/auth/iserv/login` answers 404 | The provider is not configured | Add its three variables to `.env` and restart |

### Sources

- IServ, setting up the SSO: <https://doku.iserv.de/manage/system/sso/>
- IServ, developer notes on OAuth and OpenID: <https://doku.iserv.de/development/oauth/>
- IServ discovery document of a school: `https://<iserv address>/.well-known/openid-configuration`
- Moodle plugin OAuth2 Server: <https://moodle.org/plugins/local_oauth2>
- Moodle plugin source and README: <https://github.com/enovation/moodle-local_oauth2>
- Moodle, installing plugins: <https://docs.moodle.org/en/Installing_plugins>
- OpenID Connect Core: <https://openid.net/specs/openid-connect-core-1_0.html>
- PKCE (RFC 7636): <https://datatracker.ietf.org/doc/html/rfc7636>

## Emails

Customers get three emails: payment details after ordering, their tickets as QR codes once
the payment is confirmed, and a notice if the reservation was cancelled for missing payment.

- **Sender (`MAIL_FROM`):** a noreply address, e.g. `tickets@noreply.example.com`.
- **Support (`SUPPORT_EMAIL`):** e.g. `support@example.com`. Every email names it and uses it
  as Reply-To. Set it up at the mail provider as an address that only forwards to the admin's
  own mailbox, so customers never see that address. The admin may answer from their own address.
- **Mail server:** the backend logs in to the SMTP server of the sender address like a mail
  program does, so the sender needs a real mailbox (or an SMTP account) at the mail provider,
  even if nobody reads it.

### Setting up the mail server

1. Create the sender mailbox, e.g. `tickets@noreply.example.com`, at the mail provider.
2. Look up its SMTP settings in the provider's help pages ("SMTP", "Postausgangsserver"):
   server name, port and encryption. With two-factor authentication, create an app password
   for the backend instead of using the account password.
3. Put them into `.env`:

   ```
   MAIL_FROM=tickets@noreply.example.com
   MAIL_HOST=smtp.example.com
   MAIL_USERNAME=tickets@noreply.example.com
   MAIL_PASSWORD=<password or app password>

   # Port 587 with STARTTLS (most providers):
   MAIL_PORT=587
   MAIL_TLS=false
   MAIL_START_TLS=REQUIRED

   # or port 465 with SSL/TLS:
   MAIL_PORT=465
   MAIL_TLS=true
   MAIL_START_TLS=DISABLED
   ```

   `MAIL_START_TLS=REQUIRED` refuses to send the password unencrypted. Remove
   `QUARKUS_MAILER_MOCK` if it is set.
4. Set up the support address as a forwarding address (see above).
5. Make sure the provider has SPF and DKIM set up for the sender domain (usually a DNS
   entry the provider explains); otherwise the emails often end up in spam.
6. Restart the backend, place a test order and check that the payment email arrives. If an
   email cannot be sent, the backend log shows the reason.

| What you see | Cause | Fix |
| --- | --- | --- |
| Placing an order answers 500, log: `Connection refused` or timeout | Wrong `MAIL_HOST`/`MAIL_PORT`, or the server's firewall/hoster blocks outgoing SMTP ports | Check the values; ask the hoster whether port 587/465 is open |
| Log: `535` or `authentication failed` | Wrong username or password, or an app password is required | Check the login, create an app password |
| Log mentions `STARTTLS` | The port does not offer STARTTLS | For port 465 use `MAIL_TLS=true` and `MAIL_START_TLS=DISABLED` |
| Emails arrive in spam | SPF/DKIM missing for the sender domain | Set up SPF and DKIM at the provider |

Placing an order and confirming a payment fail as long as their email cannot be sent, so
nobody ends up without the email. The cancellation email is the exception: the tickets are
released anyway and the failure is only logged.

All mail settings: [Quarkus mailer reference](https://quarkus.io/guides/mailer-reference).

## Production

### Start

The provided `docker-compose.yml` is also meant for production: put the production values
into `.env` (including `TICKET_FRONTEND_URL`, `TICKET_SSO_BACKEND_URL` and the mail server,
without `QUARKUS_MAILER_MOCK`) and run `docker compose up -d --build`. The `Dockerfile` builds
the backend inside Docker, so the server needs nothing but Docker. The database port is only
reachable from the server itself; the backend listens on port 8080 for the reverse proxy.

### HTTPS and one site

Logins are cookies that browsers only send over HTTPS and only between pages of the same
site. So in production:

- Serve website and backend over HTTPS.
- Put both on the same site: the easiest is one domain with the backend under `/api`,
  e.g. with [Caddy](https://caddyserver.com/docs/caddyfile/directives/handle_path):

```
tickets.example.com {
    handle_path /api/* {
        reverse_proxy backend:8080
    }
    reverse_proxy frontend:3000
}
```

  Then `TICKET_FRONTEND_URL=https://tickets.example.com` and
  `TICKET_SSO_BACKEND_URL=https://tickets.example.com/api`. A subdomain like
  `api.tickets.example.com` also works; a completely different domain does not.

### Time zone

Event starts, payment deadlines and entry times are stored as local time without time zone,
exactly as the admin enters them. The backend must therefore run in the school's time zone:
the `Dockerfile` sets `-Duser.timezone=Europe/Berlin`. When starting the jar some other way,
pass that option too (`java -Duser.timezone=Europe/Berlin -jar target/quarkus-app/quarkus-run.jar`).

### After the first start

1. Log in to the admin panel with `admin` / `admin` right away and set a personal password
   and email address. Until then anyone who knows the default login could do it.
2. Change the door staff password (default `einlass`) and tell it to the helpers.
3. Check payment days and the entry time in the settings.
4. Register the redirect URIs at IServ and/or Moodle and test a customer login.
5. Place a test order and check that the emails arrive.
