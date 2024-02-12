plugins {
  java
  id("com.github.johnrengelman.shadow")
  alias(catalog.plugins.taskinfo)
}

val shadowedImplementation by configurations.creating {
  // do not shadow transitive dependencies
  isTransitive = false
}

dependencies {

  // AWS cloud resource providers
  shadowedImplementation(catalog.awsContribResources)

  // TODO : GCP resource providers
  // "com.google.cloud.opentelemetry:detector-resources:0.25.2-alpha"

  // application servers resource providers
  shadowedImplementation(catalog.contribResources)
}

tasks {
  assemble {
    dependsOn(shadowJar)
  }
  jar {
    // jar not needed as the shadowed jar replaces it
    enabled = false
  }
  shadowJar {
    configurations = listOf(shadowedImplementation)

    // skip service provider definitions as providers should not be initialized by SDK.
    exclude("META-INF/services/**")
  }
}
