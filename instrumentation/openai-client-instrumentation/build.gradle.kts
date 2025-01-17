plugins {
  id("elastic-otel.instrumentation-conventions")
}

dependencies {
  compileOnly(catalog.openaiClient)
  testImplementation(catalog.openaiClient)

  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
  testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
  testImplementation("org.slf4j:slf4j-simple:2.0.16")
  testImplementation(catalog.wiremock)
}

muzzle {
  // TODO: setup muzzle to check older versions of openAI client
  // See the docs on how to do it:
  // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/muzzle.md
}

tasks.withType<Test>().configureEach {
  // The instrumentation is experimental and therefore disabled by default, it needs to be explicitly enabled
  jvmArgs("-Dotel.instrumentation.openai-client.enabled=true")
}
