FROM gradle:8.6-jdk17 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build --no-daemon

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080 9090
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/authdb
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=password
ENV JWT_SECRET=your-very-secure-secret-key
ENTRYPOINT ["java", "-jar", "app.jar"]