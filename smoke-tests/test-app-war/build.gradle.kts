plugins {
  alias(gradlePlugins.plugins.taskinfo)
  war
}

dependencies {
  compileOnly("jakarta.servlet:jakarta.servlet-api:5.0.0")
}

val packagingAttribute = Attribute.of("elastic.packaging", String::class.java)
dependencies.attributesSchema {
  attribute(packagingAttribute)
}

java {
  // java 8 since using spring boot 2.x
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  // javadoc not required
  javadoc {
    isEnabled = false
  }
  assemble {
    dependsOn(war)
  }
}

configurations.archives {
  attributes {
    // using a custom elastic attribute for variant selection as the produced one does not
    // have any by default, hence preventing selection
    attribute(packagingAttribute, "war")
  }
}
