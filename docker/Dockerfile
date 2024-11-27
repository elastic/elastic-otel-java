FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:32f06b169bb4b0f257fbb10e8c8379f06d3ee1355c89b3327cb623781a29590e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
