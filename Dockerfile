# syntax=docker/dockerfile:1.7
# ──────────────────────────────────────────────────────────────────────────
# Volcano Arts Center Platform — multi-stage build for Railway / any Docker
# host. Stage 1 compiles the Maven project against a JDK; stage 2 ships only
# a slim JRE + the fat jar. Result is ~250 MB instead of 600 MB.
# ──────────────────────────────────────────────────────────────────────────

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Cache Maven deps separately from source for faster rebuilds.
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -q dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests package

# Extract the Spring Boot jar layers for better Docker layer caching at runtime.
RUN mkdir -p /build/extracted \
    && cp target/*.jar /build/extracted/app.jar \
    && cd /build/extracted \
    && java -Djarmode=layertools -jar app.jar extract

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring \
    && apk add --no-cache curl

USER spring:spring

COPY --from=builder /build/extracted/dependencies/         ./
COPY --from=builder /build/extracted/spring-boot-loader/    ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/           ./

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom" \
    PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS "http://localhost:${PORT}/actuator/health/liveness" || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
