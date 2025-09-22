plugins {
  id("elastic-otel.agent-packaging-conventions")
  id("elastic-otel.sign-and-publish-conventions")
  id("elastic-otel.license-report-conventions")
  alias(catalog.plugins.taskinfo)
}

description = "Elastic Distribution of OpenTelemetry Java Agent"

base.archivesName.set("elastic-otel-javaagent")

publishingConventions {
  artifactTasks.add(tasks.shadowJar)
  artifactTasks.add(tasks.javadocJar)
  artifactTasks.add(tasks.sourcesJar)
}

dependencies {
  upstreamAgent(platform(catalog.opentelemetryInstrumentationAlphaBom))
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent")
}

licenseReport {
  configurations = arrayOf(
    project.configurations.bootstrapLibsClasspath.name,
    project.configurations.javaagentLibsClasspath.name
    // No need to include the upstreamAgent, because it already has all dependent licenses packaged
    // Those will be preserved in our JAR
  )
}

tasks {

  // We override the agent entrypoints defined in elastic-otel.agent-packaging-convention
  shadowJar {
    // include licenses and notices in jar
    from(fullLicenseReport) {
      into("META-INF")
    }
  }

  assemble {
    dependsOn(javadocJar, sourcesJar)
  }
}

