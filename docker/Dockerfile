FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:58a132eadb4d188bc84f87b6a6129ede122cc7bc4c1d02d68ccb26cccf1f0871
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
