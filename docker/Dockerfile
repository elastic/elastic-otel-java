FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:d05de806903139d26bd08aa7de04c6893c433f4b361674814f9a43dd74c4faee
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
