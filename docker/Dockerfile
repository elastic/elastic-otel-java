FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:d56fa50e5427f566f0a9ba8b1cad553bfca706de9223776d658e5cd608edd58b
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
