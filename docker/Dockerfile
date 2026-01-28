FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:17c8370b33443a247d2b60ca6b19c6f2fe81e4b90a2de3fa3e42665bcf7a346f
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
