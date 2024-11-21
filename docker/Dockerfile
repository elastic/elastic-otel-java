FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:973e72c78eded337fef2f5231f0a2a5b1898c8354f3d2c2d062597b1318c5620
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
