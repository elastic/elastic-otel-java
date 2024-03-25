import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

plugins {
  id("elastic-otel.agent-packaging-conventions")
  id("elastic-otel.sign-and-publish-conventions")
  alias(catalog.plugins.taskinfo)
  alias(catalog.plugins.licenseReport)
}

description = "Elastic OpenTelemetry java distribution agent"

base.archivesName.set("elastic-otel-javaagent")

publishingConventions {
  artifactTasks.add(tasks.shadowJar)
  artifactTasks.add(tasks.javadocJar)
  artifactTasks.add(tasks.sourcesJar)
}

dependencies {
  // required to access OpenTelemetryAgent
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")

  upstreamAgent(platform(catalog.opentelemetryInstrumentationAlphaBom))
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent")
}


tasks {

  val cleanLicenses by registering(Delete::class) {
    Files.list(rootDir.toPath().resolve("licenses"))
      .filter { f -> Files.isDirectory(f) || f.fileName.startsWith("NOTICE_") }
      .forEach { f -> delete(f) }
  }
  generateLicenseReport {
    dependsOn(cleanLicenses)
  }

  // We override the agent entrypoints defined in elastic-otel.agent-packaging-convention
  shadowJar {

    dependsOn(named("updateLicensesAndNotice"))

    // include licenses and notices in jar
    from(rootDir) {
      into("META-INF")

      include("LICENSE")
      include("NOTICE")
      include("licenses/**")
    }

    //TODO: The agent-for-testing should also use our custom entrypoint
    manifest {
      attributes["Main-Class"] = "co.elastic.otel.agent.ElasticAgent"
      attributes["Agent-Class"] = "co.elastic.otel.agent.ElasticAgent"
      attributes["Premain-Class"] = "co.elastic.otel.agent.ElasticAgent"
    }
  }

  assemble {
    dependsOn(javadocJar, sourcesJar)
  }

  val licensesDir = rootDir.toPath().resolve("licenses")
  val licenseFile = "more-licences.md"

  licenseReport {
    outputDir = licensesDir.toString()
    renderers = arrayOf(InventoryMarkdownReportRenderer(licenseFile))
    excludeBoms = true
    excludes = arrayOf(
      "io.opentelemetry:opentelemetry-bom-alpha",
      "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha"
    )
    filters = arrayOf(LicenseBundleNormalizer("${rootProject.rootDir}/buildscripts/license-normalizer-bundle.json", true))
    projects = arrayOf(rootProject, rootProject.project("agent"), rootProject.project("bootstrap"),
      rootProject.project("common"), rootProject.project("custom"),
      rootProject.project("inferred-spans"), rootProject.project("instrumentation"),
      rootProject.project("resources"), project)

    configurations = arrayOf("runtimeClasspath", "compileClasspath")
  }

  register("updateLicensesAndNotice") {
    dependsOn(generateLicenseReport)
    doLast {

      var year = Calendar.getInstance().get(Calendar.YEAR)
      var lines = ArrayList<String>(
        listOf(
          "Elastic OpenTelemetry Java Distribution",
          "Copyright 2023-${year} Elasticsearch B.V.",
          "",
          "This project is licensed under the Apache License, Version 2.0 - https://www.apache.org/licenses/LICENSE-2.0",
          "A copy of the Apache License, Version 2.0 is provided in the 'LICENSE' file.",
          "",
          "This project depends on ASM which is licensed under the 3-Clause BSD License - https://opensource.org/licenses/BSD-3-Clause",
          "A copy of the ASM license (3-Clause BSD License) is provided in the 'licenses/LICENSE_asm_bsd-3-clause' file.",
          "",
          "This project depends on okhttp, which contains 'publicsuffixes.gz' that is licensed under the Mozilla Public License, v. 2.0 - https://mozilla.org/MPL/2.0/",
          "A copy of the publicsuffixes.gz license (Mozilla Public License, v. 2.0) is provided in the 'licenses/LICENSE_mpl-2' file.",
          "",
          "Details on the individual dependencies notices and licenses can be found in the 'licenses' folder.",
          ""
        )
      )

      Files.write(rootDir.toPath().resolve("NOTICE"), lines)

      // make the generated license report idempotent by removing the date
      val licenseReport = licensesDir.resolve(licenseFile)
      var newLicenseLines = Files.readAllLines(licenseReport)
        .stream()
        .map { l -> if (l.startsWith("_$year")) "" else l }
        .collect(Collectors.toList())
      Files.write(licenseReport, newLicenseLines)
    }
  }
}

