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

      // required to make the IDE compile our own resource provider, won't be included as dependency
      compileOnly("io.opentelemetry.contrib:opentelemetry-aws-resources:" + libraries.versions.opentelemetryContribAlpha.get())
    }
  }

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
