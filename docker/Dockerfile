FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:64f5f928224ec0d10f9f1b93d45288f28e58cd2da2dc32992da1e30ccc546c61
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
