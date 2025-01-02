FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:eef54b3a414aa53b98f0f8df2633aed83c3ba6230722769282925442968f0364
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
