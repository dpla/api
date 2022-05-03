FROM openjdk:8

WORKDIR /opt/api
COPY target/scala-2.13/dpla-api.jar .
EXPOSE 8080
CMD ["java", "-jar", "/opt/api/dpla-api.jar"]