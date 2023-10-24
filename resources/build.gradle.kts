plugins {
  java
  alias(gradlePlugins.plugins.shadow)
  alias(gradlePlugins.plugins.taskinfo)
}

dependencies {

  implementation(project("repackaged")) {
    attributes {
      attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
    dependencies {
      // AWS cloud resource providers transitive dependencies
      implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
      implementation("com.squareup.okhttp3:okhttp:4.11.0")
    }
  }

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
