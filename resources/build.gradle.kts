plugins {
  java
  id("com.github.johnrengelman.shadow")
  alias(catalog.plugins.taskinfo)
}

dependencies {

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  compileOnly(catalog.gcpContribResources)
  compileOnly(catalog.awsContribResources)
  compileOnly(catalog.contribResources)
}
