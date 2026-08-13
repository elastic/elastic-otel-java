import org.gradle.kotlin.dsl.mavenCentral
import org.gradle.kotlin.dsl.repositories

rootProject.name = "extensions-examples"

include(":resource-attribute")
include(":modify-span")
include(":test-app")

pluginManagement {
  plugins {
    id("com.gradleup.shadow").version("9.2.2")
    id("com.diffplug.spotless").version("8.1.0")
  }
}
