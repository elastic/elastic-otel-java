FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:d4def25f2fd3b0ff9bc68091cd1d89524e41b7d3fc0d3b3a665720eb92145f3b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
