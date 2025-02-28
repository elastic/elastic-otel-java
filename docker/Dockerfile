FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6387bd4c462007eaecaf13a423aea99c8a8452da09244c129703324aa97769c6
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
