# ─────────────────────────────────────────────
#  Stage 1: Build — compile the Spring Boot app
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Cache Maven dependencies first (layer cache optimisation)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build fat-jar
COPY src ./src
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────────
#  Stage 2: Run — minimal JRE runtime image
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]