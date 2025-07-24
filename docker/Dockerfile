FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:2a601e3db11b58d6d5bfedb651c513e46556e231a56f437990ec4f0248f2207b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
