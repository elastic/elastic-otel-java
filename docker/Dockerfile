FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:30f2097053535867d06399fd08649e5ae834f4baab638803bc4fd0f1855de89d
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
