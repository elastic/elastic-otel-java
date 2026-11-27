plugins {
  application
  java

  id("org.springframework.boot") version "3.3.5"
  id("io.spring.dependency-management") version "1.1.6"
}

group = "co.elastic.tutorial"
version = "1.0.0"
description = "Sample Spring Boot app for the Elastic Observability Java monitoring tutorial"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
}

application {
  mainClass.set("co.elastic.tutorial.javamonitor.JavaMonitorDemoApplication")
}
