import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
  id("elastic-otel.library-packaging-conventions")
  id("com.gradleup.shadow")
}

dependencies {

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-exporter-logging")
  compileOnly("io.opentelemetry:opentelemetry-exporter-logging-otlp")
  compileOnly(libs.slf4j.api)
  implementation(libs.bundles.log4j2) {
    // Workaround for https://github.com/apache/logging-log4j2/issues/3754
    // TODO: Can be probably removed with log4j2 2.25.1+
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
