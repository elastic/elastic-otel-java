FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:b85d54c9019ff80e88aab7d357ede3341d1442ce190173eed42ae5a116753e4e
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
