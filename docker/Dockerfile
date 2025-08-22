FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:9161f40d9690ad840d262521328452691c342da8f9aa6573c07a87dd59950bc9
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
