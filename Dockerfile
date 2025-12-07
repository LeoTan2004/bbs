# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS builder

ARG MAVEN_SETTING

ARG MAVEN_SETTING_FILE

WORKDIR /app

# 1. Just Copy Maven Wrapper Files, to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# 2. Fix line endings and make mvnw executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN apt-get update && apt-get install -y --no-install-recommends \
        gcc g++ make

# If a `MAVEN_SETTING_FILE` build-arg is provided pointing to a
# Maven `settings.xml` file, copy it to `/root/.m2/settings.xml`
RUN if [ -n "${MAVEN_SETTING_FILE:-}" ]; then \
      mkdir -p /root/.m2 && cp "$MAVEN_SETTING_FILE" /root/.m2/settings.xml && \
      echo "Copied Maven settings from ${MAVEN_SETTING_FILE} to /root/.m2/settings.xml"; \
    else \
      echo "No MAVEN_SETTING_FILE provided; using default Maven settings"; \
    fi

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

VOLUME /app/logs

COPY --from=builder /app/target/*.jar app.jar

# 7. Create Minimal User to run the Application
RUN groupadd -r spring && useradd -r -g spring spring \
 && chown spring:spring /app

USER spring

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health/liveness || exit 1

CMD ["java", "-jar", "app.jar", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/logs/heapdump_<pid>.hprof"]