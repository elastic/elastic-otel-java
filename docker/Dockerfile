FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:fdfd7f357a09f933ab22143273849f8b247360f2f94f4dc2ea473001c59f9f0b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
