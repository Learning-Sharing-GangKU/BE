FROM gradle:8.10.1-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY . .
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jdk-jammy AS runner
WORKDIR /app

RUN groupadd -r gangku && useradd -r -g gangku gangku

ENV TZ=Asia/Seoul \
    LANG=ko_KR.UTF-8 \
    LANGUAGE=ko_KR:ko \
    LC_ALL=ko_KR.UTF-8 \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown gangku:gangku /app/app.jar

USER gangku:gangku

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]