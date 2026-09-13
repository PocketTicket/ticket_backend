# ticket_backend

Backend of a ticketing system for (school) events, built with Quarkus, jOOQ, Flyway and
PostgreSQL. How it works is described in [documentation.md](documentation.md).

## Running locally

1. Copy `.env.example` to `.env` and fill in the values. `.env` holds everything secret
   (database password, cookie key, bank account) and is never committed.
   The admin panel login of a fresh installation is `admin` / `admin`; the first login
   asks for a personal password and an email address.
2. Start the database: `docker compose up -d db`
3. Start the app: `./mvnw quarkus:dev`

Every build applies the Flyway migrations to that database and generates the jOOQ classes
from it, so the database has to be running to build. The build connects with the
`jooq.codegen.jdbc.*` values in `pom.xml` (`app`/`app`/`app`).

- Swagger UI: <http://localhost:8080/q/swagger-ui>
- In dev mode emails are not sent but written to the log.
- Reset the local database: `./mvnw flyway:clean`, then start again.

## Packaging

`./mvnw package` produces `target/quarkus-app/quarkus-run.jar`;
`src/main/docker/Dockerfile.jvm` builds a container image from it. Pass the values
from `.env` to the container as environment variables.
