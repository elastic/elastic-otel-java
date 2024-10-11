FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:277ebb42c458ef39cb4028f9204f0b3d51d8cd628ea737a65696a1143c3e42fe
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
