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
  implementation(libs.bundles.log4j2)

  // needs to be added in order to allow access to AgentListener interface
  // this is currently required because autoconfigure is currently not exposed to the extension API.
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}

tasks {
  val shadowJar by existing(ShadowJar::class) {
    // required for META-INF/services files relocation
    mergeServiceFiles()

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
