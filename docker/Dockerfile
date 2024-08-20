FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c16d3ad6cebf387e8dd2ad769f54320c4819fbbaa21e729fad087c7ae223b4d0
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
