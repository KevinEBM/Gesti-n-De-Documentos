# Build stage — compila el JAR dentro de la imagen (no usa target/ local).
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw dependency:go-offline -B

COPY src src

RUN sed -i 's/\r$//' mvnw \
    && ./mvnw package -DskipTests -B

# Runtime stage — solo JRE + artefacto ejecutable.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/gestion-documental-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
