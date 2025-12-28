FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY app.jar app.jar

EXPOSE 10001   
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
