FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:bfdeddb33330a281950c2a54adef991dbbe6a42832bc505d13b11beaf50ae73f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
