FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:1b51ff6dba78c98d3e02b0cd64a8ce3238c7a40408d21e3af12a329d44db6f23
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
