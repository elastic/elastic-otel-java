FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:55b297da5151d2a2997e8ab9729fe1304e4869389d7090ab7031cc29530f69f8
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
