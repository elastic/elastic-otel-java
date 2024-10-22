FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:2e3da56229f5673b149191a5451bb4c6ead117a307b0cc98c7a0651ca6f4523e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
