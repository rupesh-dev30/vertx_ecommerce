FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*fat.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
