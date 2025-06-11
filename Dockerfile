FROM maven:3.9.6-eclipse-temurin-17 AS build
COPY . /app
WORKDIR /app
RUN mvn dependency:copy-dependencies -DoutputDirectory=target/lib
RUN mvn package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/always-encrypted-sample-1.0-SNAPSHOT.jar .
COPY --from=build /app/target/lib ./lib
ENTRYPOINT ["java", "-cp", "always-encrypted-sample-1.0-SNAPSHOT.jar:lib/*", "com.example.AlwaysEncryptedSample"]
