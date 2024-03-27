plugins {
  java
  id("com.github.johnrengelman.shadow")
  alias(catalog.plugins.taskinfo)
}

dependencies {

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // those are already included in the upstream agent
  compileOnly(catalog.gcpContribResources)
  compileOnly(catalog.awsContribResources)

  // not included in the upstream agent
  implementation(catalog.contribResources)
}
