plugins {
  alias(catalog.plugins.muzzleGeneration)
  alias(catalog.plugins.muzzleCheck)
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
  pass {
    val openaiClientLib = catalog.openaiClient.get()
    group.set(openaiClientLib.group)
    module.set(openaiClientLib.name)
    versions.set("(,0.20.0]")
    // no assertInverse.set(true) here because we don't want muzzle to fail for newer releases on our main branch
    // instead, renovate will bump the version and failures will be automatically detected on that bump PR
  }
}
