FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4f5c987e261bed59c16d7e44c349d56edc9e82be205713fdc48a0ba55f7aa3cc
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
