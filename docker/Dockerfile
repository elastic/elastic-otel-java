FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:0833673bcb40fb65963f302651bcb8a7aaf74265cf08c6ca883d940fdf9f1eed
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
