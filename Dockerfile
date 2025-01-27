FROM amazoncorretto:11

WORKDIR /tmp

COPY aws-certs.sh .
RUN yum update -y && yum install openssl -y
RUN bash aws-certs.sh

WORKDIR /opt/api
COPY sentry.properties .
COPY sentry-opentelemetry-agent.jar .
COPY target/scala-2.13/dpla-api.jar .
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 CMD ["curl", "-f", "http://localhost:8080/health-check"]
EXPOSE 8080
CMD ["java", "-javaagent:sentry-opentelemetry-agent.jar", "-jar", "/opt/api/dpla-api.jar"]