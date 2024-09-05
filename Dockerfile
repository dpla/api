FROM amazoncorretto:11

WORKDIR /tmp

COPY aws-certs.sh .
RUN yum update -y && yum install openssl perl -y
RUN bash aws-certs.sh

WORKDIR /opt/api
COPY target/scala-2.13/dpla-api.jar .
EXPOSE 8080
CMD ["java", "-jar", "/opt/api/dpla-api.jar"]