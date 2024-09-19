import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer
import com.github.jk1.license.render.ReportRenderer

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
  into(layout.buildDirectory.dir("reports/all-licenses"))
}

licenseReport {
  allowedLicensesFile = File("${rootProject.rootDir}/buildscripts/allowed-licenses.json")

  renderers = arrayOf<ReportRenderer>(InventoryMarkdownReportRenderer("more-licences.md"))
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
