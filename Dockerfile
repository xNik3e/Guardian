FROM openjdk:17
LABEL authors="xnik3e"
EXPOSE 8080
COPY .env .env
ADD target/Guardian-0.0.1-SNAPSHOT.jar Guardian-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "Guardian-0.0.1-SNAPSHOT.jar"]