FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:661355310ab9ecc86082d247341881eb272853eb8fe4c91e53e02f0f2e30fb2c
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
