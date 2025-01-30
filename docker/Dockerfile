FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:8df3ca6e1468dfda09359c47162bff2df9f9b48986037e8324b63e9347a3ce17
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
