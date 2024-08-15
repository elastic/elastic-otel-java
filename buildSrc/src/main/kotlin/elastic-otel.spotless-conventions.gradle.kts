plugins {
  id("com.diffplug.spotless")
}

spotless {
  java {
    target("src/**/*.java")
    googleJavaFormat()

    licenseHeaderFile(rootProject.file("buildscripts/spotless.license.java"), "(package|import|public)")
      .named("default")

    licenseHeaderFile(rootProject.file("buildscripts/spotless.reallogic.license.java"), "(package|import|public)")
      .named("reallogic")
      .onlyIfContentMatches("package co.elastic.otel.profiler.collections;")

  }
}
