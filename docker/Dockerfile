FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:da434d810ab02bb0e864462f395120de182b49279bc3e68037e581ce28e11e20
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
