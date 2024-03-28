plugins {
  id("elastic-otel.agent-packaging-conventions")
}

dependencies {
  // This project uses the special "agent-for-testing" which exposes collected data in-memory
  // for validation in tests
  upstreamAgent(platform(catalog.opentelemetryInstrumentationAlphaBom))
  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-agent-for-testing")
}
