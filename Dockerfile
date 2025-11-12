# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN addgroup --system spring && adduser --system --ingroup spring spring
RUN chown -R spring:spring /app
USER spring
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]