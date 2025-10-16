FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:eb38cadc8aa4d1e5fc159d40e0fb9eb23f59e139fe27483b674ba617d52db4e9
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
