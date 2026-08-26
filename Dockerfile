# Stage 1: Build the Spring Boot application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and resolve dependencies first to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create production runtime image using lightweight JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a dedicated non-root user and group for container security
RUN addgroup -S cinexgroup && adduser -S cinexuser -G cinexgroup
USER cinexuser:cinexgroup

# Copy JAR artifact from build stage
COPY --from=build /app/target/bookmyshow-*.jar app.jar

# Expose backend port
EXPOSE 8080

# Run with container memory optimizations
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
