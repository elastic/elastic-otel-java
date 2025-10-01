FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:00fa283d3647165f65fa64324d60f09d29f0ad1ade717af349131b2fc65f8f8f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
