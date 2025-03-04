FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:15a4191ff8ec8305dcba449365e8a1586c9cda8e016ae838d960b9009c6a5cac
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
