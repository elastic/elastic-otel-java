FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c56628d8102cc34eeb4aaaf6279e88d2b23775569f9deeacc915b52f28163b8f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
