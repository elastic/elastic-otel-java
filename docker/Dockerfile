FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:0801a437e721bebdd2d5be62e3f63f60fc9d1d82efacabe4c5b8792d74ae7006
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
