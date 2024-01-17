plugins {
    id("java-library")
}

dependencies {
  implementation(project(":jvmti-access"))
  implementation(project(":common"))
  implementation("io.opentelemetry.semconv:opentelemetry-semconv")


  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly(libs.findbugs.jsr305)

  testCompileOnly(libs.findbugs.jsr305)
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation(libs.assertj.core)
}
