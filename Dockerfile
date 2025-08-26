FROM gradle:8.2.1-jdk17 AS builder

USER root
WORKDIR /app

COPY --chown=gradle:gradle build.gradle settings.gradle gradlew /app/
COPY --chown=gradle:gradle gradle /app/gradle
# debug
RUN ls -lR /app/src/main/resources

# .gradle 디렉터리 생성 및 소유권 변경
RUN mkdir -p /app/.gradle && chown -R gradle:gradle /app /home/gradle

RUN chmod +x gradlew

USER gradle

RUN ./gradlew dependencies

COPY --chown=gradle:gradle src /app/src

RUN ./gradlew build -x test --no-daemon --no-build-cache

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]