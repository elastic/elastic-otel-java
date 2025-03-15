FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:66d427a0e8c76a611ba4c5cd91301c73250f265a78be258f1d57d4457332a6fc
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
