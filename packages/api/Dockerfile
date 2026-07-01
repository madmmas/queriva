FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY target/queriva-api-*.jar app.jar

ENV API_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD curl -f http://localhost:${API_PORT}/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
