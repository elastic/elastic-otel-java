FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:2503e99eee6e33a34fe5e1e0e30b2c2e955a67c2bc4b472d0b152dabbb8213b5
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
