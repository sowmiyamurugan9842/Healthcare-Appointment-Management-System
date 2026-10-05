# Stage 1: Build the Spring Boot application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package production JAR (tests executed separately in CI)
RUN mvn clean package -DskipTests

# Stage 2: Production runtime environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for cloud security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the compiled JAR artifact from the build stage
COPY --from=build /app/target/healthcare-appointment-management-system-0.0.1-SNAPSHOT.jar app.jar

# Render assigns a dynamic port via the PORT environment variable (default fallback: 8080)
ENV PORT=8080
EXPOSE 8080

# Launch the Spring Boot application binding to Render's dynamic PORT
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
