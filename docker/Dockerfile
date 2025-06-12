FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:af257460ae20e9b5c72a20f11c4e523cf6df87c1931be4617fab5cf877790fc7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
