plugins {
    id("elastic-otel.java-conventions")
}

dependencies {
    compileOnly(catalog.openaiClient)
    compileOnly("io.opentelemetry:opentelemetry-sdk")
    compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
}
