FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6fbf07849a440c8dca9aa7e9cb56ed3ecaa9eb40f8a4f36b39393d7b32d78ecc
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
