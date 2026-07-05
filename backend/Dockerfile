# ─────────────────────────────────────────────
# Stage 1 – Build the JAR with Maven
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy Maven wrapper + pom first (layer caching)
COPY .mvn/ .mvn/
COPY pom.xml ./

# Download all dependencies (cached unless pom changes)
RUN mvn dependency:go-offline -q 2>/dev/null || true

# Copy source and build, skip tests to speed up build
COPY src/ src/
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2 – Lean runtime image
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create non-root user for security
RUN addgroup -S renthub && adduser -S renthub -G renthub

# Copy the fat JAR from builder stage
COPY --from=build /workspace/target/*.jar app.jar

# Create uploads directory for local storage fallback
RUN mkdir -p /app/uploads && chown renthub:renthub /app/uploads

USER renthub

# Railway injects PORT env var – Spring reads server.port from it
EXPOSE 8080

ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-Dserver.port=${PORT:-8080}", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
