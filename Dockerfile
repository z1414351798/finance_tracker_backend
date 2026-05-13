# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder

# Install Maven (jammy = Ubuntu 22.04, apt has Maven 3.6)
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy pom first — dependencies are cached as a separate layer.
# If only src/ changes, Maven skips the download step on rebuild.
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -B -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# eclipse-temurin:17-jre-alpine is ~180 MB vs ~400 MB for full JDK
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=builder /app/target/finance_tracker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# -Xmx512m  keeps memory usage predictable on a 4 GB server
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
