FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:d85a5e5d9670291848bf01e859b0ec74ccc978d943cd2abd015fddb36465861e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
