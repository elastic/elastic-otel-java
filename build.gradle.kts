import java.io.FileInputStream
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
  alias(catalog.plugins.nexusPublish)
}

val versionProperties = java.util.Properties()
versionProperties.load(FileInputStream(file("version.properties")))

group = "co.elastic.otel"
version = versionProperties.get("version").toString()
description = "Elastic Distribution for OpenTelemetry Java"

defaultTasks("agent:assemble")

subprojects {
  group = rootProject.group
  version = rootProject.version
}

nexusPublishing {
  repositories {
    sonatype()
  }
}

tasks {

  register("currentVersion") {
    doLast {
      println(project.version)
    }
  }

  register("setVersion") {
    doLast {
      val versionFile = file("version.properties")
      versionFile.writeText("version=${project.property("newVersion")}\n")
    }
  }

  register("setNextVersion") {
    doLast {
      val semVer = "\\d+\\.\\d+\\.\\d+".toRegex().find(version.toString())!!.value;
      val nums = semVer.split('.');

      val versionFile = file("version.properties")
      versionFile.writeText("version=${nums[0]}.${nums[1]}.${nums[2].toInt() + 1}-SNAPSHOT\n")
    }
  }
}
