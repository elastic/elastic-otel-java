FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a83a91a5c38b1c67fe12dde2ddd5bc03f094f098a100499b8140426a7901e36e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
