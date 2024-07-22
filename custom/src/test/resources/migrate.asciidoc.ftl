<#-- @ftlvariable name="config" type="java.util.Map<java.lang.String,java.util.List<co.elastic.otel.config.ConfigurationOption>>" -->
<#-- @ftlvariable name="keys" type="java.util.Collection<java.lang.String>" -->

:language: Java
:language_lc: java
:distro_name: Elastic Distribution for OpenTelemetry {language}

include::release-status.asciidoc[]

:!language:
:!language_lc:
:!distro_name:

This documentation describes how applications using the {apm-java-ref}/index.html[Elastic APM Java agent] can be updated to use the Elastic Distribution for OpenTelemetry Java ("the distro").

Start by installing the distro following the steps outlined in <<get-started>>. Then update existing APM Java agent configuration options in your application with the equivalent https://opentelemetry.io/docs/languages/sdk-configuration/general/[OpenTelemetry SDK configuration variables] (listed below).

[discrete]
[[option-reference]]
=== Option reference

This is a list of all APM Java agent configuration options grouped by their category.
Click on a key to get more information.

<#list config as category, options>
    <#list options as option>
* <<${option.key}>>
    </#list>
</#list>

<#list config as category, options>
[discrete]
[[category]]
=== ${category}

    <#list options as option>
[discrete]
[[${option.key}]]
==== `${option.key}`${option.tags?has_content?then(" (${option.tags?join(' ')})", '')}

${option.description}

////
This file is auto-generated. Please make changes in *Configuration.java (for example, CoreConfiguration.java) and execute ConfigurationExporter.
////

    </#list>
</#list>

<#macro defaultValue option>${option.defaultValueAsString?has_content?then("${option.defaultValueAsString?replace(',([^\\\\s])', ', $1', 'r')}", '<none>')}</#macro>

<#list config as category, options>
// ${category}
</#list>
