FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:568df5fb0e237c3a2139399d4bd8e80994f24321fa8469aec565b8566704e83a
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
