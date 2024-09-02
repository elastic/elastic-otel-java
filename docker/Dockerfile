FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:59b6a573ce874481d86272329426d4590f948b6714f371fb047c7fb0274f7f12
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
