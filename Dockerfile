# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS builder

ARG MAVEN_SETTING

WORKDIR /app

# 1. Just Copy Maven Wrapper Files, to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# 2. Fix line endings and make mvnw executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# If a `MAVEN_SETTING` build-arg is provided containing the contents
# of a Maven `settings.xml`, write it to `/root/.m2/settings.xml` so
# we can use it for offline dependency fetch and packaging steps.
RUN if [ -n "${MAVEN_SETTING:-}" ]; then \
      mkdir -p /root/.m2 && printf '%s' "$MAVEN_SETTING" > /root/.m2/settings.xml && \
      echo "Wrote Maven settings to /root/.m2/settings.xml"; \
    else \
      echo "No MAVEN_SETTING provided; using default Maven settings"; \
    fi

# 3. Pre-download Dependencies (use provided settings if present)
RUN if [ -f /root/.m2/settings.xml ]; then \
      ./mvnw -s /root/.m2/settings.xml dependency:go-offline -B; \
    else \
      ./mvnw dependency:go-offline -B; \
    fi

# 4. Copy Source Code And Build (use provided settings if present)
COPY src ./src
RUN if [ -f /root/.m2/settings.xml ]; then \
      ./mvnw -s /root/.m2/settings.xml clean package -DskipTests; \
    else \
      ./mvnw clean package -DskipTests; \
    fi

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