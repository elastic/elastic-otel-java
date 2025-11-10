FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c14a89d20a7d8c8159a0a915be57b09a6267ad64c1e9b830c34be3ab85f0b5c2
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
