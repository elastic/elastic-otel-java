FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:5e3d0d5d6e3470b57d2f39e72418003f17027c98ee47bcf953225e6cc1be7ba2
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
