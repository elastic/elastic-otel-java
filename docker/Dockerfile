FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:4f102b13319db859b8076e847abb15b90c6885a806c3dfae6fb146f3b33d5d0b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
