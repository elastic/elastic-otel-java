plugins {
  alias(catalog.plugins.muzzleGeneration)
  alias(catalog.plugins.muzzleCheck)
  id("elastic-otel.instrumentation-conventions")
}

dependencies {
  implementation(project(":instrumentation:openai-client-instrumentation:common"))

  testImplementation(catalog.openaiClient)
  testImplementation(project(":instrumentation:openai-client-instrumentation:testing-common"))
}

muzzle {
  pass {
    val openaiClientLib = catalog.openaiClient.get()
    group.set(openaiClientLib.group)
    module.set(openaiClientLib.name)
    versions.set("(,${openaiClientLib.version}]")
    // no assertInverse.set(true) here because we don't want muzzle to fail for newer releases on our main branch
    // instead, renovate will bump the version and failures will be automatically detected on that bump PR
  }
}
