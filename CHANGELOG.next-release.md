This file contains all changes which are not released yet.
<!--
 Note that the content between the marker comment lines (e.g. FIXES-START/END) will be automatically
 moved into the docs/release-notes markdown files on release (via the .ci/ReleaseChangelog.java script).
 Simply add the changes as bullet points into those sections, empty lines will be ignored. Example:

* Description of the change - [#1234](https://github.com/elastic/apm-agent-java/pull/1234)
-->

# Fixes
<!--FIXES-START-->

<!--FIXES-END-->
# Features and enhancements
<!--ENHANCEMENTS-START-->
* Add support for dynamic configuration options for 9.2 #818
* Switch upstream Opamp client #789

<!--ENHANCEMENTS-END-->
# Deprecations
<!--DEPRECATIONS-START-->

<!--DEPRECATIONS-END-->

# Breaking Changes
<!--BREAKING-CHANGES-START-->
* Switch to upstream instrumentation of openai by default #763

::::{dropdown} OpenAI instrumentation switched from openai-client to openai
In OpenTelemetry Java agent version 2.18.0, an `openai` instrumentation module was added. This conflicted with the `openai-client` instrumentation module that was implemented in the EDOT agent. Since the `openai` module is on by default, we switched off the `openai-client` instrumentation module (previously on by default). The functionality is broadly the same.
**Impact**<br> Small changes in span names and attributes expected. If the elastic specific `ELASTIC_OTEL_JAVA_INSTRUMENTATION_GENAI_EMIT_EVENTS` was previously set to true, this would no longer produce events.
**Action**<br> The equivalent of `ELASTIC_OTEL_JAVA_INSTRUMENTATION_GENAI_EMIT_EVENTS` is `OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT`. If you want to revert entirely to the previous setup, turn off the upstream implementation and turn on the EDOT one. For example: `OTEL_INSTRUMENTATION_OPENAI=false` and `OTEL_INSTRUMENTATION_OPENAI_CLIENT=true`
View [PR #763](https://github.com/elastic/elastic-otel-java/pull/763).
::::

<!--BREAKING-CHANGES-END-->
