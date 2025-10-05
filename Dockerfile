FROM gradle:8.10.1-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon || return 0

COPY . .
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jdk-jammy AS runner
WORKDIR /app

ENV TZ=Asia/Seoul \
    LANG=ko_KR.UTF-8 \
    LANGUAGE=ko_KR:ko \
    LC_ALL=ko_KR.UTF-8 \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
