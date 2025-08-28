FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:ed432bb5128cbeccfdcc6cbe204658d099e798cb4fb669e6b01d408c4d88f297
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
