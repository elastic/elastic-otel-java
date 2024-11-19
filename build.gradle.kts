import java.io.FileInputStream

plugins {
  alias(catalog.plugins.nexusPublish)
}

val versionProperties = java.util.Properties()
versionProperties.load(FileInputStream(file("version.properties")))

group = "co.elastic.otel"
version = versionProperties.get("version").toString()
description = "Elastic Distribution of OpenTelemetry Java"

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

repositories {
  mavenCentral()
}

val printDependencyVersions: Configuration by configurations.creating
dependencies {
  printDependencyVersions(platform(libs.opentelemetryInstrumentationAlphaBom))
  printDependencyVersions("io.opentelemetry.javaagent:opentelemetry-javaagent")
  printDependencyVersions("io.opentelemetry:opentelemetry-sdk")
}

tasks {

  fun getResolvedDependency(identifier: String): ModuleComponentIdentifier? {
    return printDependencyVersions.incoming.resolutionResult.allComponents.mapNotNull {
      val id = it.id
      return@mapNotNull if (id is ModuleComponentIdentifier) id else null;
    }.find {
      it.moduleIdentifier.toString() == identifier
    }
  }

  /**
   * Used from within our release automation as part of the release note generation.
   */
  register("printUpstreamDependenciesMarkdown") {
    dependsOn(printDependencyVersions)
    doLast {
      println("* opentelemetry-javaagent: `" + getResolvedDependency("io.opentelemetry.javaagent:opentelemetry-javaagent")!!.version + "`")
      println("* opentelemetry-sdk: `" + getResolvedDependency("io.opentelemetry:opentelemetry-sdk")!!.version + "`")
      println("* opentelemetry-semconv: `" + libs.versions.opentelemetrySemconvAlpha.get() + "`")
      println("* opentelemetry-java-contrib: `" + libs.versions.opentelemetryContribAlpha.get() + "`")
    }
  }

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
