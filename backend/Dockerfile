# syntax=docker/dockerfile:1
FROM gradle:9.9.1-jdk21 AS build
WORKDIR /src
COPY settings.gradle.kts build.gradle.kts ./
RUN gradle dependencies --no-daemon -q > /dev/null || true
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
