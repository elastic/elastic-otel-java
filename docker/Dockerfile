FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:1235a5eee51eb21cd63b0dec7d65c439119c4a6020c0d2d86dc1bf3e41797568
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
