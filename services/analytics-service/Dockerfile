FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","/app/app.jar"]
