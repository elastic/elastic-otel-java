FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:0e6ec4eb5f662805911c232be9fe134e38e692f553ebf5ad33c1833d09e64310
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
