plugins {
  java
  alias(gradlePlugins.plugins.shadow)
  alias(gradlePlugins.plugins.taskinfo)
}

val shadowedImplementation by configurations.creating {
  // allows to inherit common dependencies (mostly boms for now)
  extendsFrom(configurations.implementation.get())
  // do not shadow transitive dependencies
  isTransitive = false
}

dependencies {
  // AWS cloud resource providers
  shadowedImplementation("io.opentelemetry.contrib:opentelemetry-aws-resources:" + libraries.versions.opentelemetryContribAlpha.get())
  // application servers resource providers
  shadowedImplementation("io.opentelemetry.contrib:opentelemetry-resource-providers:" + libraries.versions.opentelemetryContribAlpha.get())

  // TODO : GCP resource providers
  // "com.google.cloud.opentelemetry:detector-resources:0.25.2-alpha"

  // TODO : ensure the transitive dependencies of this project are preserved and the produced artifact still has them

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}

// programmatically create config for dependencies to shadow without their transitive dependencies
//for (dep in project.configurations.getByName("implementation").allDependencies) {
//  if (dep.group.equals("io.opentelemetry.contrib")) {
//    shadowedImplementation.dependencies.add(dep)
//  }
//}

// TODO : how do we expose the shadowed artifact to be used by other modules ?
// canBeResolved = false --> define the dependency
// canBeResolved = true --> the dependency graph can be resolved

// canBeConsumed = true --> declare the configuration to be usable by other components
// canBeConsumed = false -->

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
