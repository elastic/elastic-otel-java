plugins {
    id("elastic-otel.test-with-agent-conventions")
}

dependencies {
  testImplementation(project(":common"))
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
}

javaVersionTesting {
  enableTestsOnOpenJ9 = false
}

tasks.withType<Test>() {
  jvmArgs(
    //"-Dotel.javaagent.debug=true",
    "-Dotel.service.name=testing",
    "-Delastic.otel.inferred.spans.enabled=true",
    "-Delastic.otel.inferred.spans.duration=2000ms",
    "-Delastic.otel.inferred.spans.interval=2000ms",
    "-Delastic.otel.inferred.spans.sampling.interval=5ms"
  )
}
