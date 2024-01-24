plugins {
  `java-library`
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly(libs.findbugs.jsr305)
  implementation("com.lmax:disruptor:3.4.4")
  implementation("org.jctools:jctools-core:4.0.1")
  implementation(project(":common"))

  testCompileOnly(libs.findbugs.jsr305)
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation(libs.assertj.core)
  testImplementation(libs.awaitility)
  testImplementation("org.kohsuke:github-api:1.133")
  testImplementation("org.apache.commons:commons-compress:1.21")
  testImplementation("tools.profiler:async-profiler:1.8.3")
}

tasks.withType<Test>().all {
  jvmArgs("-Djava.util.logging.config.file="+sourceSets.test.get().output.resourcesDir+"/logging.properties")
}


publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
      versionMapping {
        usage("java-api") {
          fromResolutionOf("runtimeClasspath")
        }
        usage("java-runtime") {
          fromResolutionResult()
        }
      }
    }
  }
}
