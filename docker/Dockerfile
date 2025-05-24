FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:3d19648819612728a676ab4061edfb3283bd7117a22c6c4479ee1c1d51831832
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
