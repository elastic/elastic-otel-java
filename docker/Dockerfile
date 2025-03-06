FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6dcddd8855466f9e0439ae1e4b6d6ca89d8f86eee1e84e6686f4c273410d615b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
