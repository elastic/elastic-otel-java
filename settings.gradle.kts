pluginManagement {
  repositories {
    gradlePluginPortal()
    maven {
      name = "sonatype"
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }
}

rootProject.name = "elastic-otel-java"

include("agent")
include("bootstrap")
include("custom")
include("instrumentation")
include("smoke-tests")
include("testing")


//include("extension")

//include("test-app")