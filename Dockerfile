# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM gradle:8-jdk21-alpine AS builder

WORKDIR /app

# Copiar fuentes
COPY . .

# Compilar sin tests
RUN gradle :applications-app-service:bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario no-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/applications/app-service/build/libs/applications-app-service.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
