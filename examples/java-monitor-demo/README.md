# Java monitoring example

A small Spring Boot app instrumented with the Elastic Distribution of OpenTelemetry (EDOT) Java agent. It exposes a normal endpoint, an artificially slow one, and one that fails until you fix it, plus a background job that logs on a timer. That's enough traffic to see traces, metrics, and logs together in one place, and to practice finding and fixing a real failure using the data.

This example is the companion app for the [Monitor a Java application](https://www.elastic.co/docs/solutions/observability/get-started/other-tutorials/tutorial-monitor-java-application) tutorial, which walks through sending its data to Elastic and diagnosing the failure end-to-end. Use this README if you just want to build and run the app on its own.

## Build

```bash
./gradlew bootJar
```

This produces `build/libs/java-monitor-demo-1.0.0.jar`.

## Install the EDOT Java agent

Download the agent jar from Maven Central, picking the [latest release](https://github.com/elastic/elastic-otel-java/releases/latest):

```bash
curl -L -O https://repo1.maven.org/maven2/co/elastic/otel/elastic-otel-javaagent/<version>/elastic-otel-javaagent-<version>.jar
```

## Run

```bash
OTEL_SERVICE_NAME=java-monitor-demo \
  java -javaagent:./elastic-otel-javaagent-<version>.jar -jar build/libs/java-monitor-demo-1.0.0.jar
```

If an OTLP collector is already listening on `localhost:4317`/`localhost:4318` (for example, an Elastic Agent configured as an OTel Collector, or a local OpenTelemetry Collector), the agent finds it with no further configuration. Otherwise, set `OTEL_EXPORTER_OTLP_ENDPOINT` to point at your collector or Elastic endpoint.

## Endpoints

- `GET /greeting?item=mug`: a normal request. Returns the item's stock level.
- `GET /slow?item=mug`: simulates a slow downstream warehouse lookup (0.8-3s). Good for seeing latency in a trace.
- `GET /error?item=mug`: simulates calling a pricing service. Fails with `503` until you fix it (see below).

Items available: `mug`, `t-shirt`, `sticker-pack`.

## Find and fix the failure

`/error` returns `503` by default: the simulated pricing service is disabled. Stop the app, then restart it with `PRICING_SERVICE_ENABLED=true`:

```bash
OTEL_SERVICE_NAME=java-monitor-demo \
PRICING_SERVICE_ENABLED=true \
  java -javaagent:./elastic-otel-javaagent-<version>.jar -jar build/libs/java-monitor-demo-1.0.0.jar
```

`/error` now returns `200` with a price. In a real app this would be a code or config fix; here it's a single environment variable so the scenario stays simple and repeatable.

## Generate traffic

```bash
./scripts/generate-traffic.sh [host:port]
```

Defaults to `localhost:8080` and sends a steady mix of normal, slow, and failing requests every couple of seconds. Stop it with `Ctrl+C`.

## Run with Docker

```bash
docker build -t java-monitor-demo .
docker run -p 8080:8080 \
  -e OTEL_SERVICE_NAME=java-monitor-demo \
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4318 \
  java-monitor-demo
```

The Dockerfile expects `build/libs/java-monitor-demo-1.0.0.jar` to already exist, so run `./gradlew bootJar` first. It doesn't bundle the EDOT Java agent. Add it with a `-javaagent` flag in a custom image, or attach it via your container platform's Java agent injection if available.
