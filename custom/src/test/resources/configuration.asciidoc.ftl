<#-- @ftlvariable name="config" type="java.util.Map<java.lang.String,java.util.List<co.elastic.otel.config.ConfigurationOption>>" -->
<#-- @ftlvariable name="keys" type="java.util.Collection<java.lang.String>" -->
[[configuration]]
== Configuration

This section outlines the most similar configuration of the Elastic Opentelemetry
distribution APM Java agent to the https://www.elastic.co/guide/en/apm/agent/java/current/index.html[Elastic APM Java agent].

The Elastic Opentelemetry distribution APM Java agent uses all the standard
https://opentelemetry.io/docs/concepts/sdk-configuration/general-sdk-configuration/[Opentelemetry Java agent configuration].

[horizontal]

[float]
=== Option reference

This is a list of all configuration options grouped by their category.
Click on a key to get more information.

<#list config as category, options>
* <<config-${category?lower_case?replace(" ", "-")}>>
    <#list options as option>
** <<config-${option.key?replace("[^a-z]", "-", "r")}>>
    </#list>
</#list>

<#list config as category, options>
[[config-${category?lower_case?replace(" ", "-")}]]
=== ${category} configuration options

++++
<titleabbrev>${category}</titleabbrev>
++++

    <#list options as option>
// This file is auto generated. Please make your changes in *Configuration.java (for example CoreConfiguration.java) and execute ConfigurationExporter
[float]
[[config-${option.key?replace("[^a-z]", "-", "r")}]]
==== `${option.key}`${option.tags?has_content?then(" (${option.tags?join(' ')})", '')}

${option.description}

    </#list>
</#list>

<#macro defaultValue option>${option.defaultValueAsString?has_content?then("${option.defaultValueAsString?replace(',([^\\\\s])', ', $1', 'r')}", '<none>')}</#macro>

----
<#list config as category, options>
############################################
# ${category?right_pad(40)} #
############################################

</#list>
----
