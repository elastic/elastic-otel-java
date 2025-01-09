FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:f2813ac649de3b3e90bfed50723c59cbfba803fd8d021bd522fc3dc4802b0cdb
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
