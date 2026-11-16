# Builds the service from source and packages it into a small runtime image.
# Used by deploy.sh (Cloud Build) and works locally with `docker build .`.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Download dependencies in their own layer so source changes rebuild quickly.
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# Tests are run separately (`mvn test`); they are skipped here to keep image builds fast.
RUN ./mvnw -B -q package -DskipTests \
    && cp target/jasaks-*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# Cloud Run tells the container which port to listen on through $PORT.
CMD ["sh", "-c", "exec java -jar app.jar --server.port=${PORT:-8080}"]
