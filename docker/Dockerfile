FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:c66fdafe581a6ab1668a962015de4ce4666a60ed601d24f019f03bb4aaab8eeb
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
