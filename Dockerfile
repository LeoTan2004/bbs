# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# 1. Just Copy Maven Wrapper Files, to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# 2. Fix line endings and make mvnw executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# 3. Pre-download Dependencies
RUN ./mvnw dependency:go-offline -B

# 4. Copy Source Code And Build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# 7. Create Minimal User to run the Application
RUN groupadd -r spring && useradd -r -g spring spring \
 && chown spring:spring /app

USER spring

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]