FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:e3ed8310016ba3e4d29127b10a6200ad536a7a2318e27872c2fff760d4ffb6e5
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
