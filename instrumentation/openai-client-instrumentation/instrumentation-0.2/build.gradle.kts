plugins {
  alias(catalog.plugins.muzzleGeneration)
  alias(catalog.plugins.muzzleCheck)
  id("elastic-otel.instrumentation-conventions")
}

val openAiVersion = "0.21.1" +".13.0"; // DO NOT UPGRADE, string operations are used to prevent renovate upgrades

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
    versions.set("(,${openAiVersion}]")
    assertInverse.set(true)
  }
}
