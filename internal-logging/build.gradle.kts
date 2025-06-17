import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
  id("elastic-otel.library-packaging-conventions")
  id("com.gradleup.shadow")
}

dependencies {

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
  compileOnly(libs.slf4j.api)
  implementation(libs.bundles.log4j2) {
    // this is a optional, provided transitive dependency requiring Java 11
    // for some reason gradle still tries to resolve it and then complains about it not being java 8 compatible
    exclude(group = "com.github.spotbugs", module = "spotbugs-annotations")
  }

}

tasks {
  val shadowJar by existing(ShadowJar::class) {
    // required for META-INF/services files relocation
    mergeServiceFiles()

    // Excluding property source SPI prevents log4j system properties and env variables that might
    // be set at the application level to change the behavior of this internal log4j instance.
    exclude("**/*.log4j.util.PropertySource")

    transform(Log4j2PluginsCacheFileTransformer::class.java)

    // relocate slf4j and log4j for internal logging to prevent any conflict
    relocate("org.slf4j", "co.elastic.otel.logging.slf4j")
    relocate("org.apache.logging.log4j", "co.elastic.otel.logging.log4j")
    relocate("org.apache.logging.slf4j", "co.elastic.otel.logging.log4j.slf4j")
  }

  assemble {
    dependsOn(shadowJar)
  }
}
