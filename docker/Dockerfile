FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:ad2e15a6b7fbd893990fd9bd39fb0f367282a9ba65e350655540e470858ef382
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
