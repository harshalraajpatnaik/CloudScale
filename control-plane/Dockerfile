# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S cloudscale && adduser -S cloudscale -G cloudscale
USER cloudscale:cloudscale
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
