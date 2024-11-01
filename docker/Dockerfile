FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:74385d2e67fb0df0df261e57ea174bb2df6e858110ecd1e8928d06f53c9efa4b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
