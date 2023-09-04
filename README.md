# Elastic OpenTelemetry agent

This is currently implemented as an agent extension, created from the example provided in [opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/extension).

## Build

- extension jar: run `./gradlew build`, extension jar file will be in `build/libs/`.
- extended agent (with embedded extension): run `./gradlew extendedAgent`, extended agent will be in `build/libs/`.

## Run

- extension jar:

  ```bash
  java -javaagent:path/to/opentelemetry-javaagent.jar \
  -Dotel.javaagent.extensions=build/libs/extension-1.0-all.jar
  -jar myapp.jar
     ```

- extended agent:

  ```bash
  java -javaagent:build/lib/opentelemetry-javaagent.jar \
  -jar myapp.jar
     ```



