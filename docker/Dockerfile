FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:1c7f5aa0e7997455b8500d095c7a90e617102d3941eb0757ac62cfea509e09b9
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
