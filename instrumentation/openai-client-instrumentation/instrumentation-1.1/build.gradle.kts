plugins {
  alias(catalog.plugins.muzzleGeneration)
  alias(catalog.plugins.muzzleCheck)
  id("elastic-otel.instrumentation-conventions")
}

dependencies {
  compileOnly(catalog.openaiClient)
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  testImplementation(catalog.openaiClient)
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
  testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3")
  testImplementation("org.slf4j:slf4j-simple:2.0.17")
  testImplementation(catalog.wiremockjre8)
}

muzzle {
  pass {
    val openaiClientLib = catalog.openaiClient.get()
    group.set(openaiClientLib.group)
    module.set(openaiClientLib.name)
    versions.set("[1.1.0,${openaiClientLib.version}]")
    // no assertInverse.set(true) here because we don't want muzzle to fail for newer releases on our main branch
    // instead, renovate will bump the version and failures will be automatically detected on that bump PR
  }
}
