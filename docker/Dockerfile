FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:410bbae8ff97716b1ef803c00156cd47dab351a8075144fd4485ebcd51c41fe0
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
