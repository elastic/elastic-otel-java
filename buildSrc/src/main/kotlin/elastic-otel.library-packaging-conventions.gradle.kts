plugins {
  `java-library`
  id("elastic-otel.java-conventions")
}

tasks {

  jar {
    // include licenses and notices in jar
    from("${rootDir}") {
      into("META-INF")

      include("LICENSE")
      include("NOTICE")
    }
  }

}
