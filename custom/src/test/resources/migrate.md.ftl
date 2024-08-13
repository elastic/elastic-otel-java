<#-- @ftlvariable name="config" type="java.util.Map<java.lang.String,java.util.List<co.elastic.otel.config.ConfigurationOption>>" -->
<#-- @ftlvariable name="keys" type="java.util.Collection<java.lang.String>" -->

This documentation describes how applications using the [Elastic APM Java agent](https://www.elastic.co/guide/en/apm/agent/java/current/index.html) can be updated to use the Elastic Distribution of OpenTelemetry Java (EDOT Java).

Start by installing EDOT Java following the steps outlined in [Get started](./get-started.md). Then update existing APM Java agent configuration options in your application with the equivalent [OpenTelemetry SDK configuration variables](https://opentelemetry.io/docs/languages/sdk-configuration/general/) (listed below).

## Option reference

This is a list of all APM Java agent configuration options grouped by their category.
Click on a key to get more information.

<#list config as category, options>
    <#list options as option>
* [${option.key}](#${option.key})
    </#list>
</#list>

<#list config as category, options>
## ${category}

    <#list options as option>
### `${option.key}`${option.tags?has_content?then(" (${option.tags?join(' ')})", '')}

${option.description}

<!--
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
-->

    </#list>
</#list>

<#macro defaultValue option>${option.defaultValueAsString?has_content?then("${option.defaultValueAsString?replace(',([^\\\\s])', ', $1', 'r')}", '<none>')}</#macro>

<#list config as category, options>
<!-- ${category} -->
</#list>
