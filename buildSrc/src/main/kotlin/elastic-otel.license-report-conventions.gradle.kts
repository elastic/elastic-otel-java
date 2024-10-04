import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer
import com.github.jk1.license.render.ReportRenderer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

plugins {
  id("com.github.jk1.dependency-license-report")
}

val fullLicenseReport = tasks.register("fullLicenseReport", Copy::class) {
  dependsOn(tasks.generateLicenseReport)
  //We want to fail in case there are any non-approved licenses
  dependsOn(tasks.checkLicense)

  from(rootProject.rootDir) {
    include("NOTICE", "LICENSE")
  }
  from(licenseReport.outputDir) {
    include("**/*")
    exclude("dependencies-without-allowed-license.json")
    exclude("project-licenses-for-check-license-task.json")
    into("licenses")
  }
  val outputDir = layout.buildDirectory.dir("reports/all-licenses")
  into(outputDir)

  // make the generated license report idempotent by removing the date
  doLast {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    val reportMarkdown = Paths.get(outputDir.get().file("licenses/more-licenses.md").asFile.path)
    val newLicenseLines = Files.readAllLines(reportMarkdown)
      .stream()
      .map { l -> if (l.startsWith("_$year")) "" else l }
      .collect(Collectors.toList())
    Files.write(reportMarkdown, newLicenseLines)
  }
}

licenseReport {
  allowedLicensesFile = File("${rootProject.rootDir}/buildscripts/allowed-licenses.json")

  renderers = arrayOf<ReportRenderer>(InventoryMarkdownReportRenderer("more-licenses.md"))
  excludeBoms = true
  excludes = arrayOf(
    "io.opentelemetry:opentelemetry-bom-alpha",
    "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha"
  )
  filters = arrayOf(
    LicenseBundleNormalizer(
      "${rootProject.rootDir}/buildscripts/license-normalizer-bundle.json",
      true
    )
  )
}
