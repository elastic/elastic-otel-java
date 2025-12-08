FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:1038c51d3e88154a59aea6476f19030ceca257cddcae66abe1d047de7a5ce578
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
