FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4a629c926ff6682fea43cb167bd1eee528667332dbe97afac7d4ae0f591fe4f8
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
