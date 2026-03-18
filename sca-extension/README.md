# sca-extension — Software Composition Analysis for EDOT Java

Automatically discovers every JAR loaded by the JVM at runtime, extracts library metadata, and emits one OpenTelemetry log event per unique JAR to Elasticsearch via OTLP. The events land in the `logs-sca-default` data stream where an Elasticsearch ingest pipeline can enrich them with CVE data from the OSV database using an enrich processor that matches on `library.purl`.

## How it works

1. **Discovery** — registers a `ClassFileTransformer` with the JVM `Instrumentation` object. For every loaded class the transformer reads `ProtectionDomain.getCodeSource().getLocation()` to obtain the owning JAR path. This never transforms bytecode and never blocks the class-loading thread.

2. **Back-fill** — on startup, `Instrumentation.getAllLoadedClasses()` is scanned to capture JARs loaded before the transformer registered.

3. **Deduplication** — a `ConcurrentHashMap` keyed on JAR path ensures each JAR is processed exactly once.

4. **Async metadata extraction** — new JAR paths are placed in a bounded queue (capacity 500). A single daemon thread reads from the queue, opens each JAR, and extracts metadata using three sources in priority order:
   - `META-INF/maven/*/*/pom.properties` — most reliable for `groupId`, `artifactId`, `version`
   - `META-INF/MANIFEST.MF` — `Implementation-Title`, `Implementation-Version`, `Specification-Version`
   - Filename pattern — `name-version.jar` best-effort parse

5. **SHA-256 fingerprint** — computed from JAR bytes for exact CVE matching.

6. **Rate-limited emission** — log records are emitted at a configurable rate (default: 10 JARs/second) using a token-bucket style sleep on the background thread.

## Emitted log record

| Field | OTel attribute | Example |
|---|---|---|
| Body | — | `com.google.guava:guava:32.1.3-jre` |
| Library name | `library.name` | `guava` |
| Library version | `library.version` | `32.1.3-jre` |
| Maven groupId | `library.group_id` | `com.google.guava` |
| Package URL | `library.purl` | `pkg:maven/com.google.guava/guava@32.1.3-jre` |
| JAR path | `library.jar_path` | `/opt/app/lib/guava-32.1.3-jre.jar` |
| SHA-256 | `library.sha256` | `a1b2c3...` |
| Classloader | `library.classloader` | `jdk.internal.loader.ClassLoaders$AppClassLoader` |
| Event name | `event.name` | `library.loaded` |
| Event domain | `event.domain` | `sca` |

The instrumentation scope is `co.elastic.otel.sca`.

## Configuration

All properties can be set as JVM system properties or environment variables.

| System property | Env var | Default | Description |
|---|---|---|---|
| `elastic.otel.sca.enabled` | `ELASTIC_OTEL_SCA_ENABLED` | `true` | Enable / disable the extension |
| `elastic.otel.sca.skip_temp_jars` | `ELASTIC_OTEL_SCA_SKIP_TEMP_JARS` | `true` | Skip JARs under `java.io.tmpdir` (e.g. JRuby, Groovy generated JARs) |
| `elastic.otel.sca.jars_per_second` | `ELASTIC_OTEL_SCA_JARS_PER_SECOND` | `10` | Maximum JAR events emitted per second |

Example:

```bash
java -javaagent:elastic-otel-javaagent.jar \
     -Delastic.otel.sca.enabled=true \
     -Delastic.otel.sca.jars_per_second=20 \
     -jar myapp.jar
```

## Build & packaging

The module is built with `elastic-otel.library-packaging-conventions` and is included in the agent as an `implementation` dependency of the `custom` module — the same path taken by `inferred-spans`. No changes to the agent packaging convention are required.

```
agent (elastic-otel-javaagent.jar)
  └── custom  (javaagentLibs)
        └── sca-extension  (transitive implementation dep)
```

The two SPI registrations in `META-INF/services/` are merged into `inst/META-INF/services/` inside the agent JAR by the `mergeServiceFiles()` step in `elastic-otel.agent-packaging-conventions`.

## Downstream enrichment

The recommended Elasticsearch ingest pipeline uses an enrich processor that joins `library.purl` with an OSV-sourced enrich index:

```json
{
  "enrich": {
    "policy_name": "osv-cve-by-purl",
    "field": "library.purl",
    "target_field": "vulnerability",
    "ignore_missing": true
  }
}
```

This adds `vulnerability.cve`, `vulnerability.severity`, and `vulnerability.fix_available` fields to each log document, enabling a full library inventory with CVE status visible in Kibana.
