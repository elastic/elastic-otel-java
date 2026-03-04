FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:52799bf31ce800d69bfb0f0750e080b8dd6f8c5e54a6a4a124bad89c6768562f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
