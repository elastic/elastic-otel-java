// If this project is moved or renamed, remember to update the exclude in renovate.json
plugins {
  alias(catalog.plugins.muzzleGeneration)
  alias(catalog.plugins.muzzleCheck)
  id("elastic-otel.instrumentation-conventions")
}

val openAiVersion = "0.13.0"; // DO NOT UPGRADE

dependencies {
  compileOnly("com.openai:openai-java:${openAiVersion}")
  implementation(project(":instrumentation:openai-client-instrumentation:common"))

  testImplementation("com.openai:openai-java:${openAiVersion}")
  testImplementation(project(":instrumentation:openai-client-instrumentation:testing-common"))
}

muzzle {
  pass {
    group.set("com.openai")
    module.set("openai-java")
    versions.set("[0.8,${openAiVersion}]")
    assertInverse.set(true)
  }
}
