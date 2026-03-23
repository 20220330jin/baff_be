FROM gradle:8.2.1-jdk17 AS builder

USER root
WORKDIR /app

# Render가 루트에 생성한 application-local.yml을 빌드 이미지의 최종 목적지로 미리 복사
COPY --chown=gradle:gradle application-local.yml /app/src/main/resources/application-local.yml

COPY --chown=gradle:gradle build.gradle settings.gradle gradlew /app/
COPY --chown=gradle:gradle gradle /app/gradle

# .gradle 디렉터리 생성 및 소유권 변경
RUN mkdir -p /app/.gradle && chown -R gradle:gradle /app /home/gradle

RUN chmod +x gradlew

USER gradle

RUN ./gradlew dependencies

COPY --chown=gradle:gradle src /app/src

RUN ./gradlew build -x test --no-daemon --no-build-cache

FROM openjdk:17.0.2-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]