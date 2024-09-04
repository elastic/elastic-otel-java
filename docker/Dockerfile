FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:aad4cd4e5f6d849691748c6933761889db1a20a57231613b98bbff61fa7723ab
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
