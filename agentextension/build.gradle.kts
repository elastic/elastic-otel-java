plugins {
  id("java")
  id("elastic-otel.sign-and-publish-conventions")
  id("com.github.johnrengelman.shadow")
}

description = "Bundles all elastic extensions in a fat-jar to be used" +
    " with the vanilla agent via the otel.javaagent.extensions config option"
base.archivesName.set("elastic-otel-agentextension")

val shadowDependencies: Configuration by configurations.creating

dependencies {
  shadowDependencies(project(":custom"))
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
    dependsOn(project(":agent").tasks.named("updateLicensesAndNotice"))
    from(rootDir) {
      into("META-INF")

      include("LICENSE")
      include("NOTICE")
      include("licenses/**")
    }
  }
}
