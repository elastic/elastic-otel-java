plugins {
  java
  id("elastic-otel.spotless-conventions")
}

java {
  withJavadocJar()
  withSourcesJar()
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    name = "mavenCentralSnapshots"
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
  }
}

//https://github.com/gradle/gradle/issues/15383
val catalog = extensions.getByType<VersionCatalogsExtension>().named("catalog")
dependencies {

  implementation(platform(catalog.findLibrary("opentelemetryInstrumentationAlphaBom").get()))

  annotationProcessor(catalog.findLibrary("autoservice.processor").get())
  compileOnly(catalog.findLibrary("autoservice.annotations").get())
  compileOnly(catalog.findLibrary("findbugs.jsr305").get())

  testAnnotationProcessor(catalog.findLibrary("autoservice.processor").get())
  testCompileOnly(catalog.findLibrary("autoservice.annotations").get())
  testCompileOnly(catalog.findLibrary("findbugs.jsr305").get())
  testImplementation(catalog.findLibrary("assertj.core").get())
  testImplementation(catalog.findLibrary("awaitility").get())
  testImplementation(catalog.findLibrary("mockito").get())
  testImplementation(enforcedPlatform(catalog.findLibrary("junitBom").get()))
  testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
  test {
    useJUnitPlatform()
  }

  compileJava {
    options.release.set(8)
  }
  compileTestJava {
    options.release.set(8)
  }
}


interface JavaVersionTestingExtension {
  /**
   * This option can be set to false to disable testing with openj9 on this project
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
          if (useJ9) {
            implementation.set(JvmImplementation.J9)
          } else {
            vendor.set(JvmVendorSpec.ADOPTIUM) //Translates to temurin, also supports early-access builds
            implementation.set(JvmImplementation.VENDOR_SPECIFIC)
          }
          languageVersion.set(JavaLanguageVersion.of(testJavaVersion.majorVersion))
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
