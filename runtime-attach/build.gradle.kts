plugins {
  id("elastic-otel.java-conventions")
  id("elastic-otel.sign-and-publish-conventions")
}

description = "Elastic Distribution of OpenTelemetry Java Agent - runtime attach"

base.archivesName.set("elastic-otel-runtime-attach")

val agent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  implementation(catalog.contribRuntimeAttach)
  agent(project(":agent"))
}

tasks {
  jar {
    inputs.files(agent)
    from({
      agent.singleFile
    })
    rename("^(.*)\\.jar\$", "edot-agent.jar")
  }
}
