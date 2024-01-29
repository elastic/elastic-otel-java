dependencyResolutionManagement {
  versionCatalogs {
    create("catalog") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
