FROM node:24-bookworm-slim AS web
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN --mount=type=secret,id=build_ca if [ -f /run/secrets/build_ca ]; then export NODE_EXTRA_CA_CERTS=/run/secrets/build_ca; fi; npm ci --no-audit --no-fund --fetch-retries=1 --fetch-timeout=30000
COPY frontend/ ./
RUN npm run test && npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/ backend/
COPY --from=web /build/frontend/dist/ frontend/dist/
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml --mount=type=secret,id=build_ca \
    if [ -f /run/secrets/build_ca ]; then \
      awk 'BEGIN {n=0} /BEGIN CERTIFICATE/ {n++} {print > ("/tmp/build-ca-" n ".pem")}' /run/secrets/build_ca; \
      for cert in /tmp/build-ca-*.pem; do keytool -importcert -noprompt -trustcacerts -alias "$(basename "$cert")" -file "$cert" -cacerts -storepass changeit; done; \
      rm -f /tmp/build-ca-*.pem; \
    fi; mvn -B -f backend/pom.xml verify

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 10001 prism && useradd --uid 10001 --gid prism --no-create-home prism && mkdir -p /app/data && chown prism:prism /app/data
WORKDIR /app
COPY --from=build --chown=prism:prism /build/backend/target/prism-1.0.0.jar app.jar
USER prism
ENV PRISM_BIND=0.0.0.0 PORT=8080 PRISM_DATABASE_URL="jdbc:h2:file:/app/data/prism;DB_CLOSE_ON_EXIT=FALSE"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080; printf "GET /api/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3; head -1 <&3 | grep -q "200"'
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
