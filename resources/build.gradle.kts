plugins {
  java
  alias(catalog.plugins.shadow)
  alias(catalog.plugins.taskinfo)
}

dependencies {

  implementation(project("repackaged")) {
    attributes {
      attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
    dependencies {
      // AWS cloud resource providers transitive dependencies
      // TODO remove the static version dependencies, either by automatically getting the
      // transitive dependencies of the shaded artifacts, or by reusing the common versions that
      // are very likely provided through transitive dependencies
      implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
      implementation("com.squareup.okhttp3:okhttp:4.11.0")

      // required to make the IDE compile our own resource provider, won't be included as dependency
      compileOnly("io.opentelemetry.contrib:opentelemetry-aws-resources:" + catalog.versions.opentelemetryContribAlpha.get())
      compileOnly("io.opentelemetry.contrib:opentelemetry-resource-providers:" + catalog.versions.opentelemetryContribAlpha.get())
    }
  }

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
