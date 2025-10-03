FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:5d0f094bdbe4fcc12416da5d48fdcafef1aecc101088038b6392231c697d9038
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
