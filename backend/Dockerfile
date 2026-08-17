FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# The CI artifact is named digital-library-backend-<version>.jar.
ARG JAR_FILE=*.jar
COPY ${JAR_FILE} app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8000

ENTRYPOINT ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]
