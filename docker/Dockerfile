FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:36a56e3d8a40080f81e3ccca6b0fed0167716225bbc305052db6d5a9f594ccb2
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
