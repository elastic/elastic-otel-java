FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:b2134dbfbcfb987eec9c249fc81111963de596ff76098775c2444a2869401d9c
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
