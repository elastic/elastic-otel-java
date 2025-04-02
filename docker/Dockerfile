FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:29150cd940cc7f69407d978d5a19c86f4d9e67cf44e4d6ded787a497e8f27c9a
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
