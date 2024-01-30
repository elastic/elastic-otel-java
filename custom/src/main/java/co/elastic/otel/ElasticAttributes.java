/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel;

import io.opentelemetry.api.common.AttributeKey;

public interface ElasticAttributes {
  AttributeKey<Long> SELF_TIME = AttributeKey.longKey("elastic.span.self_time");
  AttributeKey<String> LOCAL_ROOT_ID = AttributeKey.stringKey("elastic.span.local_root.id");
  AttributeKey<String> LOCAL_ROOT_NAME = AttributeKey.stringKey("elastic.local_root.name");
  AttributeKey<String> LOCAL_ROOT_TYPE = AttributeKey.stringKey("elastic.local_root.type");
  AttributeKey<Boolean> IS_LOCAL_ROOT = AttributeKey.booleanKey("elastic.span.is_local_root");
  AttributeKey<String> SPAN_TYPE = AttributeKey.stringKey("elastic.span.type");
  AttributeKey<String> SPAN_SUBTYPE = AttributeKey.stringKey("elastic.span.subtype");

  // TODO : replace this with semantic conventions v1.24.0 equivalent once released
  AttributeKey<String> SPAN_STACKTRACE = AttributeKey.stringKey("code.stacktrace");

  // TODO : replace this with semantic conventions 1.22.0+ once released and upstream agent updated
  AttributeKey<String> TELEMETRY_DISTRO_NAME = AttributeKey.stringKey("telemetry.distro.name");
  AttributeKey<String> TELEMETRY_DISTRO_VERSION =
      AttributeKey.stringKey("telemetry.distro.version");
}
