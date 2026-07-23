FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY backend/pom.xml ./pom.xml
COPY backend/sky-common ./sky-common
COPY backend/sky-pojo ./sky-pojo
COPY backend/sky-server ./sky-server
RUN mvn -pl sky-server -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /src/sky-server/target/sky-server-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
