FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:26caa6beaee2bbf739a82e91a35173892dfe888d0a744b9e46cdc19a90d8656f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
