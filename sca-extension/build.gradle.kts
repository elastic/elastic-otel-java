plugins {
  id("elastic-otel.library-packaging-conventions")
}

description = "Elastic SCA (Software Composition Analysis) extension for OpenTelemetry Java"

tasks.compileJava {
  options.encoding = "UTF-8"
}

tasks.javadoc {
  options.encoding = "UTF-8"
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  // AutoConfiguredOpenTelemetrySdk is not yet exposed through the extension API
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
