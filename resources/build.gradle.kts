plugins {
  java
  alias(gradlePlugins.plugins.shadow)
  alias(gradlePlugins.plugins.taskinfo)
}

val shadowedImplementation by configurations.creating {
  extendsFrom(configurations.implementation.get())
  isTransitive = false
}

dependencies {
  // AWS cloud resource providers
  shadowedImplementation("io.opentelemetry.contrib:opentelemetry-aws-resources:" + libraries.versions.opentelemetryContribAlpha.get())
  implementation("io.opentelemetry.contrib:opentelemetry-aws-resources:" + libraries.versions.opentelemetryContribAlpha.get())
  // application servers resource providers
  shadowedImplementation("io.opentelemetry.contrib:opentelemetry-resource-providers:" + libraries.versions.opentelemetryContribAlpha.get())
  implementation("io.opentelemetry.contrib:opentelemetry-resource-providers:" + libraries.versions.opentelemetryContribAlpha.get())

  // TODO : GCP resource providers
  // "com.google.cloud.opentelemetry:detector-resources:0.25.2-alpha"

  // TODO : ensure the transitive dependencies of this project are preserved and the produced artifact still has them

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
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
    archiveClassifier = null
    configurations = listOf(shadowedImplementation)

    // skip service provider definitions as providers should not be initialized by SDK.
    exclude("META-INF/services/**")

  }
}


