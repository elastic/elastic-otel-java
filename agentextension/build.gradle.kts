plugins {
  id("elastic-otel.java-conventions")
  id("elastic-otel.sign-and-publish-conventions")
  id("elastic-otel.license-report-conventions")
  id("com.github.johnrengelman.shadow")
}

description = "Bundles all elastic extensions in a fat-jar to be used" +
    " with the vanilla agent via the otel.javaagent.extensions config option"
base.archivesName.set("elastic-otel-agentextension")

val shadowDependencies: Configuration by configurations.creating

dependencies {
  shadowDependencies(project(":custom"))
}

publishingConventions {
  artifactTasks.add(tasks.shadowJar)
  artifactTasks.add(tasks.javadocJar)
  artifactTasks.add(tasks.sourcesJar)
}

licenseReport {
  configurations = arrayOf(shadowDependencies.name)
}

tasks {

  jar {
    enabled = false
    dependsOn(shadowJar)
  }

  shadowJar {
    configurations = listOf(shadowDependencies)
    mergeServiceFiles()
    archiveClassifier.set("")

    // include licenses and notices in jar
    from(fullLicenseReport) {
      into("META-INF")
    }
  }
}
