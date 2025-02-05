plugins {
  id("elastic-otel.java-conventions")
}

dependencies {
  compileOnly(catalog.openaiClient)
  compileOnly(project(":instrumentation:openai-client-instrumentation:common"))
  implementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
  implementation("io.opentelemetry:opentelemetry-sdk-testing")

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
  implementation("org.slf4j:slf4j-simple:2.0.16")
  implementation(catalog.wiremock)
}
