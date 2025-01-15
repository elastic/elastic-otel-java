plugins {
  id("elastic-otel.instrumentation-conventions")
}

dependencies {
  compileOnly("com.openai:openai-java:0.11.2")

  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
  testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
  testImplementation("com.openai:openai-java:0.8.1")
  testImplementation("org.slf4j:slf4j-simple:2.0.16")
  testImplementation(catalog.wiremock)
}


tasks.withType<Test>().configureEach {
  // The instrumentation is experimental and therefore disabled by default, it needs to be explicitly enabled
  jvmArgs("-Dotel.instrumentation.openai-client.enabled=true")
}
