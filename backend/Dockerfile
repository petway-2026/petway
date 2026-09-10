FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src ./src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system petway \
    && useradd --system --gid petway --home-dir /app --shell /usr/sbin/nologin petway

WORKDIR /app

COPY --from=build --chown=petway:petway \
    /workspace/build/libs/petway-backend-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER petway
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
