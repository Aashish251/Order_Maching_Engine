# ── Stage 1: Build ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer caching — only re-downloads
# dependencies when pom.xml changes, not on every code change)
COPY .mvn/  .mvn/
COPY mvnw   pom.xml ./

RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: don't run as root
RUN addgroup -S ome && adduser -S ome -G ome
USER ome

WORKDIR /app

# Copy only the JAR from build stage (multi-stage = smaller image)
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check — Docker will mark container unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxGCPauseMillis=1", \
  "-Xms256m", \
  "-Xmx1g", \
  "-XX:+AlwaysPreTouch", \
  "-XX:+DisableExplicitGC", \
  "-Djava.net.preferIPv4Stack=true", \
  "-Dspring.profiles.active=docker", \
  "-jar", "app.jar"]