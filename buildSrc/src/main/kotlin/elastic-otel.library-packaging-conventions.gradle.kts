plugins {
  id("java")
}

tasks {

  jar {
    // ensure jar artifacts have a copy of license and notice
    from("${rootDir}/LICENSE").into("META-INF")
    from("${rootDir}/NOTICE").into("META-INF")
  }

}
