FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a37e513992a1ef58f04794bfe7ae514e3a77b2224f7e248cf3fc7e8d0f8a49e1
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
