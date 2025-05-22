plugins {
  id("elastic-otel.library-packaging-conventions")
  id("java-library")
}

dependencies {
  api(libs.okhttp)
  implementation("com.google.protobuf:protobuf-java:3.25.7")
  implementation("com.dslplatform:dsl-json-java8:1.10.0")
  implementation(libs.protobuf.util)
  implementation("com.github.f4b6a3:uuid-creator:6.1.1")
  compileOnly("com.google.auto.value:auto-value-annotations:1.11.0")
  annotationProcessor("com.google.auto.value:auto-value:1.11.0")
  testImplementation("org.mockito:mockito-junit-jupiter:4.11.0")
  testImplementation(libs.mockito)
  testImplementation(libs.assertj.core)
  testImplementation("org.mockito:mockito-inline:4.11.0")

}

tasks.test {
  useJUnitPlatform()
}
