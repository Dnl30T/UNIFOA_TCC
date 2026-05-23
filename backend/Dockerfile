# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache ca-certificates java-cacerts \
	&& update-ca-certificates

# Copy wrapper + build files first (layer cache for dependency downloads)
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon

# Copy source and build the fat JAR
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache ca-certificates java-cacerts \
	&& update-ca-certificates

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
