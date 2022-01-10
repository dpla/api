FROM openjdk:8

WORKDIR /opt/ebook-api
COPY target/scala-2.13/dpla-ebooks-api.jar .
EXPOSE 8080
CMD ["java", "-jar", "/opt/ebook-api/dpla-ebooks-api.jar"]