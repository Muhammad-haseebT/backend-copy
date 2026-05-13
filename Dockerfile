FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Step 1: Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Step 2: Copy source and build
COPY src src
RUN mvn clean package -DskipTests -B

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the target folder
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 7860
ENTRYPOINT ["java", "-jar", "app.jar"]
