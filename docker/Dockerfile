FROM docker.elastic.co/wolfi/chainguard-base:latest@sha256:a51a1cd55a717b170058395f65cd16c3ede419b4fd400f0abaae7244c7a421f9
ARG JAR_FILE
ARG EXTENSION_JAR_FILE
COPY ${JAR_FILE} /javaagent.jar
COPY ${EXTENSION_JAR_FILE} /extensions/elastic-otel-agentextension.jar 
RUN chmod go+r /javaagent.jar /extensions
