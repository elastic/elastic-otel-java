plugins {
    id("elastic-otel.test-with-agent-conventions")
}

dependencies {
  testImplementation(project(":common"))
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
}

tasks.test {
  jvmArgs(
    //"-Dotel.javaagent.debug=true"
  )
}
