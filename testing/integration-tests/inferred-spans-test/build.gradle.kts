plugins {
    id("elastic-otel.test-with-agent-convention")
}

dependencies {
  testImplementation(project(":common"))
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
}

tasks.test {
  jvmArgs(
    //"-Dotel.javaagent.debug=true",
    "-Delastic.otel.inferred.spans.enabled=true",
    "-Delastic.otel.inferred.spans.duration=2000ms",
    "-Delastic.otel.inferred.spans.interval=2000ms",
    "-Delastic.otel.inferred.spans.sampling.interval=5ms"
  )
}
