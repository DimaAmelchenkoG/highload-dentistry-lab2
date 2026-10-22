FROM eclipse-temurin:21-jdk-alpine AS build
ARG MODULE
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY config-server config-server
COPY eureka-server eureka-server
COPY gateway gateway
COPY catalog-service catalog-service
COPY appointment-service appointment-service
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon :${MODULE}:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
ARG MODULE
RUN addgroup -S dentistry && adduser -S dentistry -G dentistry && apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/${MODULE}/build/libs/*.jar app.jar
COPY config-repo /config
USER dentistry
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
