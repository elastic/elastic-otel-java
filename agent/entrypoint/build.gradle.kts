plugins {
  id("elastic-otel.java-conventions")
}

dependencies {
  // required to access OpenTelemetryAgent
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")

}
