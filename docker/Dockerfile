FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:46fd486e4716d592aa1ad7dad15bb3e6d0aa5b08b2070e9daec9f6b8c8c990a6
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
