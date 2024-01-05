plugins {
    id("java-library")
}

dependencies {
    annotationProcessor(libs.autoservice.processor)
    compileOnly(libs.autoservice.annotations)
    compileOnly("io.opentelemetry:opentelemetry-sdk")
    compileOnly(libs.findbugs.jsr305)
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")

    testAnnotationProcessor(libs.autoservice.processor)
    testCompileOnly(libs.autoservice.annotations)
    testCompileOnly(libs.findbugs.jsr305)
    testImplementation("io.opentelemetry:opentelemetry-sdk")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
    testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")
    //testImplementation("io.opentelemetry:opentelemetry-api-events")
    testImplementation(libs.assertJ.core)
}
