FROM openjdk:21-jdk

COPY build/libs/*.jar application.jar
COPY ./src/main/resources/*.yml ./

CMD ["java", "-jar", "application.jar"]
EXPOSE 80