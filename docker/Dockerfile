FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6a3678e9bb481ee585c32c8d374efad5e63d49d8a9d19daec397fabeba45cc8c
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
