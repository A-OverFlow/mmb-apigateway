FROM openjdk:21-jdk

COPY build/libs/*.jar application.jar
COPY ./src/main/resources/apigateway.yml apigateway.yml

CMD ["java", "-jar", "application.jar", "--spring.config.location=file:apigateway.yml"]

EXPOSE 80