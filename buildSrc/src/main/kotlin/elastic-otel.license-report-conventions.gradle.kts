import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer

plugins {
  id("com.github.jk1.dependency-license-report")
}

val fullLicenseReport = tasks.register("fullLicenseReport", Copy::class) {
  dependsOn(tasks.generateLicenseReport)
  from(rootProject.rootDir) {
    include("NOTICE", "LICENSE")
  }
  from(licenseReport.outputDir) {
    include("**/*")
    into("licenses")
  }
  into(layout.buildDirectory.dir("reports/all-licenses"))
}

licenseReport {

  renderers = arrayOf(InventoryMarkdownReportRenderer("more-licences.md"))
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
