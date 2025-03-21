plugins {
  id("elastic-otel.library-packaging-conventions")
}

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
    rename("^(.*)\\.jar\$", "otel-agent.jar")
  }
}
