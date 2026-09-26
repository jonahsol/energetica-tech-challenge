FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY services services
RUN chmod +x gradlew \
	&& ./gradlew :services:ingest:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/services/ingest/build/install/ingest /app
ENTRYPOINT ["/app/bin/ingest"]
