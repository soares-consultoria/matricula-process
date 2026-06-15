# Estágio de build
FROM gradle:8.14.2-jdk21 AS build
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle bootJar --no-daemon

# Estágio de runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
