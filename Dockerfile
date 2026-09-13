# Builds and runs the backend; used by docker compose (see README.md).

# Build: jOOQ generates its classes from a real database, so this stage starts a throwaway
# PostgreSQL (the same version as in docker-compose.yml) and builds against it.
FROM postgres:18-alpine AS build

RUN apk add --no-cache openjdk21-jdk
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk

WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

# The Maven wrapper may have Windows line endings after a checkout on Windows.
RUN --mount=type=cache,target=/root/.m2 \
    sed -i 's/\r$//' mvnw .mvn/wrapper/maven-wrapper.properties \
    && mkdir /tmp/pgdata && chown postgres /tmp/pgdata \
    && gosu postgres initdb -D /tmp/pgdata --auth=trust > /dev/null \
    && gosu postgres pg_ctl -D /tmp/pgdata -o "-c listen_addresses=localhost" -w start > /dev/null \
    && psql -U postgres -h localhost -c "CREATE USER app PASSWORD 'app'" -c "CREATE DATABASE app OWNER app" \
    && sh mvnw -B package \
    && gosu postgres pg_ctl -D /tmp/pgdata -w stop > /dev/null

# Run
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

ENV LANGUAGE='en_US:en'

# Four layers, so a code change does not rebuild the library layers
COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185

# Event starts and payment deadlines are stored as local time, so the backend runs in the school's time zone.
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Duser.timezone=Europe/Berlin"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
