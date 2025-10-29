FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:83f8ecdb6f089d2d393cd8138eb95f73d6ca9e29c66efd9f3b3f448d9b1a45b5
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
