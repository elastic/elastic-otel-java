plugins {
  `java-library`
  id("elastic-otel.library-packaging-conventions")
  id("elastic-otel.sign-and-publish-conventions")
}

description = rootProject.description + " inferred-spans"

dependencies {
  annotationProcessor(libs.autoservice.processor)
  compileOnly(libs.autoservice.annotations)
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly(libs.findbugs.jsr305)
  implementation("com.lmax:disruptor:3.4.4")
  implementation("org.jctools:jctools-core:4.0.1")
  implementation(project(":common"))

  testAnnotationProcessor(libs.autoservice.processor)
  testCompileOnly(libs.autoservice.annotations)
  testCompileOnly(libs.findbugs.jsr305)
  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation(libs.awaitility)
  testImplementation("org.kohsuke:github-api:1.133")
  testImplementation("org.apache.commons:commons-compress:1.21")
  testImplementation("tools.profiler:async-profiler:1.8.3")
}

tasks.compileJava {
  options.encoding = "UTF-8"
}

tasks.javadoc {
  options.encoding = "UTF-8"
}

tasks.processResources {
  doLast {
    val resourcesDir = sourceSets.main.get().output.resourcesDir
    val packageDir = resourcesDir!!.resolve("co/elastic/otel/profiler");
    packageDir.mkdirs();
    packageDir.resolve("inferred-spans-version.txt").writeText(project.version.toString())
  }
}

tasks.withType<Test>().all {
  jvmArgs("-Djava.util.logging.config.file="+sourceSets.test.get().output.resourcesDir+"/logging.properties")
}
