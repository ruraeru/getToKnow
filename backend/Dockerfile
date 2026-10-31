FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
