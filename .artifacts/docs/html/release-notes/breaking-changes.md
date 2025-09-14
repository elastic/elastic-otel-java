---
title: Elastic Distribution of OpenTelemetry Java breaking changes
description: Breaking changes for Elastic Distribution of OpenTelemetry Java.
url: https://docs-v3-preview.elastic.dev/release-notes/breaking-changes
products:
  - Elastic Cloud Serverless
  - Elastic Distribution of OpenTelemetry SDK
  - Elastic Observability
---

# Elastic Distribution of OpenTelemetry Java breaking changes

Breaking changes can impact your applications, potentially disrupting normal operations and their monitoring. Before you upgrade, carefully review the Elastic Distribution of OpenTelemetry Java breaking changes and take the necessary steps to mitigate any issues.
<dropdown title="OpenAI instrumentation switched from openai-client to openai">
  In OpenTelemetry Java agent version 2.18.0, an `openai` instrumentation module was added. This conflicted with the `openai-client` instrumentation module that was implemented in the EDOT agent. Since the `openai` module is on by default, we switched off the `openai-client` instrumentation module (previously on by default). The functionality is broadly the same.
  **Impact** Small changes in span names and attributes expected. If the elastic specific `ELASTIC_OTEL_JAVA_INSTRUMENTATION_GENAI_EMIT_EVENTS` was previously set to true, this would no longer produce events.
  **Action** The equivalent of `ELASTIC_OTEL_JAVA_INSTRUMENTATION_GENAI_EMIT_EVENTS` is `OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT`. If you want to revert entirely to the previous setup, turn off the upstream implementation and turn on the EDOT one. For example: `OTEL_INSTRUMENTATION_OPENAI=false` and `OTEL_INSTRUMENTATION_OPENAI_CLIENT=true`
  View [PR #763](https://github.com/elastic/elastic-otel-java/pull/763).
</dropdown>
