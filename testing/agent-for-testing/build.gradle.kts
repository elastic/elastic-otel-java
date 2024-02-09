plugins {
  id("elastic-otel.agent-packaging")
}

dependencies {
  // This project uses the special "agent-for-testing" which exposes collected data in-memory
  // for validation in tests
  upstreamAgent(catalog.opentelemetryJavaagentForTesting)
}
