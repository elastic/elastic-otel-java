plugins {
  id("java")
}

afterEvaluate {
  val testJavaVersion = gradle.startParameter.projectProperties["testJavaVersion"]?.let(JavaVersion::toVersion)
  val useJ9 = gradle.startParameter.projectProperties["testJavaVM"]?.run { this == "openj9" }
    ?: false
  tasks.withType<Test>().configureEach {
    if (testJavaVersion != null) {
      javaLauncher.set(
        javaToolchains.launcherFor {
          languageVersion.set(JavaLanguageVersion.of(testJavaVersion.majorVersion))
          implementation.set(if (useJ9) JvmImplementation.J9 else JvmImplementation.VENDOR_SPECIFIC)
        }
      )

      //only run tests which have a compatible bytecode version
      val compileVersion = JavaVersion.toVersion(project.tasks.compileTestJava.get().options.release.get())
      isEnabled = isEnabled && testJavaVersion.isCompatibleWith(compileVersion)
    }
  }
}
