FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY services services
RUN chmod +x gradlew \
	&& ./gradlew :services:api:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/services/api/build/libs/*.jar /app/api.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-Xmx256m"
ENTRYPOINT ["java", "-jar", "/app/api.jar"]
