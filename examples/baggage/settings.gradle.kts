rootProject.name = "baggage-example"

include(":extension")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("com.gradleup.shadow").version("9.3.0")
    id("com.diffplug.spotless").version("8.1.0")
    id("io.opentelemetry.instrumentation.muzzle-generation").version("2.23.0-alpha")
    id("io.opentelemetry.instrumentation.muzzle-check").version("2.23.0-alpha")
  }

}
