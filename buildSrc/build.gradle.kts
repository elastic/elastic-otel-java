plugins {
  `kotlin-dsl`
}
repositories {
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }

    // maven central snapshots for upstream opentelemetry
    maven {
        name = "mavenCentralSnapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation(catalog.spotlessPlugin)
    implementation(catalog.licenseReportPlugin)
    implementation(catalog.muzzleGenerationPlugin)
    implementation(catalog.muzzleCheckPlugin)
    implementation(catalog.shadowPlugin)
    // The ant dependency is required to add custom transformers for the shadow plugin
    // but it is unfortunately not exposed as transitive dependency
    implementation(catalog.ant)
    // ASM is used for compile-time code modification to inject a field for backing SpanValues
    implementation(catalog.asm)
    // TODO : for now we have to disable it because it transitively imports an older apache httpclient
    // that makes the transitive one from jib fail see https://github.com/elastic/elastic-otel-java/issues/9 for details
    // implementation("io.opentelemetry.instrumentation:gradle-plugins:1.30.0-alpha-SNAPSHOT")
}
