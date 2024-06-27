plugins {
  id("java")
}

interface JavaVersionTestingExtension {
  /**
   * By default the convention will publish the artifacts and pom as libraries.
   * To override the behaviour provide the tasks producing the artifacts as this property.
   * This should only be required when publishing fat-jars with custom packaging.
   */
  val enableTestsOnOpenJ9: Property<Boolean>
}

val javaVersionTesting = project.extensions.create<JavaVersionTestingExtension>("javaVersionTesting")
javaVersionTesting.enableTestsOnOpenJ9.convention(true)

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
      //Disable if the test does not support openJ9
      isEnabled = isEnabled && (!useJ9 || javaVersionTesting.enableTestsOnOpenJ9.get())
    }
  }
}
