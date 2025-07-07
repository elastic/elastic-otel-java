FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c709f502d7d35ffb3d9c6e51a4ef3110ec475102501789a4dc0da5a173df7688
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
