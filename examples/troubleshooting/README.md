# Troubleshooting EDOT agent setups

This directory holds code examples for testing aspects of running applications
with the EDOT Java agent

# Compilation

Currently the examples are just standalone code. The 
dependencies are the opentelemetry jars, so putting any
of the code examples into a project and adding these three
dependencies will allow for easy compilation

- `io.opentelemetry:opentelemetry-api`
- `io.opentelemetry:opentelemetry-sdk`
- `io.opentelemetry:opentelemetry-exporter-otlp`

(Manual compilation would require the jars and their dependencies downloaded, and then javac -cp <list of jars> path-to-java-file.)

# Traces, Metrics, Logs

The examples in this section all take the same three arguments:
1. The name of the service you want to have displayed in the APM UI
2. The endpoint to send traces to, normally the Elastic APM server or the OpenTelemetry collector.
   The url would typically look like `http://localhost:4318/v1/traces` or `https://somewhere:443/v1/traces`
   but if `/v1/traces` is missing from the argument, it is added to the endpoint
3. The secret token or apikey in the format secret:<token> or apikey:<apikey>.
   If the argument doesn't start with neither `secret:` nor `apikey:`, then the full argument is
   assumed to be a secret token

## Traces

The [`TestOtelSdkTrace`](./src/main/java/elastic/troubleshooting/TestOtelSdkTrace.java) class is a standalone class that creates
a span (named `test span`) in a service that you name. 

After running the class, you should see the trace in the APM UI, eg with service name set to `test1` and
correct endpoint and token, you should see something similar to 
![this](images\test-trace.png)

## Metrics

The [`TestOtelSdkMetrics`](./src/main/java/elastic/troubleshooting/TestOtelSdkMetrics.java) class is a standalone class that generates
metrics for `jvm.thread.count` for a little over 3 minutes in a service that you name.

After running the class, you should see the metrics in the APM UI, eg with service name set to `test1` and
correct endpoint and token, you should see something similar to
![this](images\test-metrics.png)

## Logs

To do
