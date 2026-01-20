FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a42fd0f8865a04b8ab4ff12e9ecbba681ebae4112b9e7e9e34ce9b08ebfa650d
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
