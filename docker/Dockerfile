FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a02075b9fd57b4c0ad04232054569eccbcab254159deba01cc4c9fcbeb800411
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
