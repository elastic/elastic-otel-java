FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:9ded4d2364e7f263cada56b0b9ca3ef643e8dac958a79df3d18c2a9f0a33fbc7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
