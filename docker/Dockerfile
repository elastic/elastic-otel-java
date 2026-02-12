FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a71dfa7c4cb17fb0f6814cda77e0bdf7b66dec9b6725fe61abd29c2263775af7
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
