plugins {
  id("java")
  id("elastic-otel.sign-and-publish-conventions")
}

description = "Bundles all elastic extensions in a fat-jar to be used" +
    " with the vanilla agent via the otel.javaagent.extensions config option"
base.archivesName.set("elastic-otel-agentextension")

val fatjar: Configuration by configurations.creating

dependencies {
  fatjar(project(":custom"))
}


tasks {
  jar {
    dependsOn(fatjar)
    dependsOn(project(":agent").tasks.named("updateLicensesAndNotice"))

    // include licenses and notices in jar
    from(rootDir) {
      into("META-INF")

      include("LICENSE")
      include("NOTICE")
      include("licenses/**")
    }
    from(fatjar.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}
