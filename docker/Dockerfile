FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:d74b1fda6b7fee2c90b410df258e005c049e0672fe16d79d00e58f14fb69f90b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
