FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:6b2446102599dee9d17c472ccdb63b462def06028900fdcbf08d413074f8d131
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
