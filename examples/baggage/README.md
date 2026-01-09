# Baggage examples

This is an elementary application providing an end-to-end example usage of [OpenTelemetry baggage](https://opentelemetry.io/docs/concepts/signals/baggage/).

It is split in two services: a gateway and a backend
- an HTTP backend service running on http://localhost:9000/backend/
- an HTTP gateway service running on http://localhost:8000/gateway/

The gateway is in charge of authentication and delegates to the backend by providing a technical ID for the backend to use for authentication.

In this example, we use the OpenTelemetry Baggage to add the customer ID and name to the captured spans and logs.

The baggage entries are copied to the span and log attributes by using the following configuration options:
- `otel.java.experimental.span-attributes.copy-from-baggage.include` for spans
- `otel.java.experimental.log-attributes.copy-from-baggage.include` for logs

The following baggate entries are used in this example:
- `example.customer.id`: the "technical" customer ID, set by the gateway
- `example.customer.name`: the "friendly" customer name, set by the gateway. This value is not available in the backend without using baggage.
- `example.gateway.http.route`: Only available when using [extension](#gateway-extension). This entry provides a copy of the `http.route` attribute from the gateway span.

## Build

```bash
./gradlew clean assemble
```

## Configure

The javaagent is already packaged and included in the application by using [runtime attach](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/java/setup/runtime-attach) feature.

OTel configuration must be provided through environment variables or JVM system properties.

## Run

### Backend

The backend application does not use any OpenTelemetry API, thus on a real-world application no code changes are required.

```shell
export OTEL_SERVICE_NAME='backend'
java \
-Dotel.java.experimental.span-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-Dotel.java.experimental.log-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-jar ./build/libs/baggage-app.jar backend
```

### Gateway (Baggage API)

By default, the gateway application will use the OpenTelemetry Baggage API, and thus a real-world application
would need minor code changes to implement this. See [example below for an alternative](#gateway-extension) without application modification.

```shell
export OTEL_SERVICE_NAME='gateway'
java \
-Dotel.java.experimental.span-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-Dotel.java.experimental.log-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-jar ./build/libs/baggage-app.jar gateway
```

### Gateway (extension)

Using the OpenTelemetry Baggage API requires code changes in the gateway application, this might not be always possible or desirable.
As an alternative, we can use a dedicated agent extension to implement the same functionality without modifying the application code.

```shell
export OTEL_SERVICE_NAME='gateway'
java \
-Dotel.java.experimental.span-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-Dotel.java.experimental.log-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-Dotel.javaagent.extensions=./extension/build/libs/baggage-extension.jar \
-jar ./build/libs/baggage-app.jar gateway no-baggage-api
```

## Execute requests

Once both services are started, you can execute queries on the gateway with queries like the following:
```shell
curl -H 'Authorization: secret=12345' http://localhost:8000/gateway/
```

This will create the following log messages on `gateway` and `backend` respectively:
```
gateway request for customer ID = 12345
backend request for customer ID = 12345
```

All the spans and logs captured within the scope of the baggage will have the following attributes:
- `example.customer.name` with value `my-awesome-customer-12345`
- `example.customer.id` with value `12345`

While the value of `example.customer.id` could potentially be extracted by parsing the log message,
the value of `example.customer.name` is only available through the baggage. Also, both are being applied
consistently to all spans and logs within the gateway request trace.

As a result, it is now possible to use those custom attributes to filter data and create
dedicated dashboards. The only code modification required is in the gateway code.

## Limit baggage propagation

By default, all baggage entities are propagated to downstream services like the context propagation IDs (trace ID, span ID, etc).

This behavior might not be desirable as it could lead to leaking information to downstream services which are not 
trusted or outside your control.

It is however possible to implement a custom baggage propagator to filter out which services should receive which baggage entries.
An example implementation `FilteringBaggagePropagator` is provided in the [extension](./extension), it requires to be enabled
by replacing `baggage` propagator with `filtering-baggage` in the `otel.propagators` configuration.

For example, running the gateway with the following configuration DOES NOT progagate any baggage to the backend
because only `localhost` is allowed, and not `127.0.0.1`. This will however still capture the baggage as span
attributes in the outgoing HTTP request span on the gateway.

```shell
java \
-Dotel.java.experimental.span-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-Dotel.java.experimental.log-attributes.copy-from-baggage.include=example.customer.id,example.customer.name,example.gateway.http.route \
-Dotel.javaagent.extensions=./extension/build/libs/baggage-extension.jar \
-Dotel.propagators="tracecontext,baggage-filtering" \
-jar ${folder}/build/libs/baggage-app.jar gateway no-baggage-api http://127.0.0.1:9000/backend/
```
