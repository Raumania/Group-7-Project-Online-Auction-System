FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .

RUN mvn clean package -pl server -am -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/server/target/bidmaster-server-jar-with-dependencies.jar app.jar

EXPOSE 3636

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]