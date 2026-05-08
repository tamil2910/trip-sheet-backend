# Build
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# RUN
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# ENTRYPOINT [ "java", "-jar", "app.jar" ]
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]