plugins {
  id("elastic-otel.library-packaging-conventions")
}

dependencies {

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
  compileOnly(libs.slf4j.api)
  implementation(libs.bundles.log4j2)

  // needs to be added in order to allow access to AgentListener interface
  // this is currently required because autoconfigure is currently not exposed to the extension API.
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}

tasks {
}
