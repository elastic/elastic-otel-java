plugins {
  java
  id("com.github.johnrengelman.shadow")
  alias(catalog.plugins.taskinfo)
}

dependencies {

  implementation(project("repackaged")) {
    attributes {
      attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
    dependencies {
      // AWS cloud resource providers transitive dependencies
      implementation(catalog.jackson)
      implementation(catalog.okhttp)

      // required to make the IDE compile our own resource provider, won't be included as dependency
      compileOnly(catalog.awsContribResources)
      compileOnly(catalog.contribResources)
    }
  }

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
