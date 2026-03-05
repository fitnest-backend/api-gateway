FROM gradle:9.3-jdk25 AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY . .
RUN ./gradlew clean bootJar --no-build-cache --no-daemon

FROM eclipse-temurin:25-jre

RUN groupadd -g 1001 fitnest && \
    useradd -u 1001 -g fitnest -m -s /bin/bash fitnest

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R fitnest:fitnest /app /tmp
USER fitnest

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]



