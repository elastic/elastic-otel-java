FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:792313d45d114da11e169252586f76a79ff20248c7af4283f11b94bff7f1aed7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
