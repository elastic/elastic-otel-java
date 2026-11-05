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
package co.elastic.otel.spanname;

import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

public final class SpanNamingSpanExporter implements SpanExporter {

  // span name used by HTTP instrumentation when the request method is not a known one
  private static final String UNKNOWN_METHOD_SPAN_NAME = "HTTP";

  private final SpanExporter delegate;
  private final SpanNameSettings settings;

  public SpanNamingSpanExporter(SpanExporter delegate, SpanNameSettings settings) {
    this.delegate = delegate;
    this.settings = settings;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    boolean usePath = settings.usePathAsSpanName();
    List<SpanNameSettings.NameGroup> groups = settings.spanNameGroups();
    if (!usePath && groups.isEmpty()) {
      return delegate.export(spans);
    }

    List<SpanData> result = new ArrayList<>(spans.size());
    boolean renamed = false;
    for (SpanData span : spans) {
      String newName = spanName(span, usePath, groups);
      if (newName != null) {
        result.add(new RenamedSpanData(span, newName));
        renamed = true;
      } else {
        result.add(span);
      }
    }
    return delegate.export(renamed ? result : spans);
  }

  @Nullable
  private static String spanName(
      SpanData span, boolean usePath, List<SpanNameSettings.NameGroup> groups) {
    String name = span.getName();
    boolean changed = false;
    if (usePath) {
      String nameWithPath = nameWithPath(span);
      if (nameWithPath != null) {
        name = nameWithPath;
        changed = true;
      }
    }
    for (int i = 0; i < groups.size(); i++) {
      SpanNameSettings.NameGroup group = groups.get(i);
      if (group.matcher.matches(name)) {
        name = group.name;
        changed = true;
        break;
      }
    }
    return changed ? name : null;
  }

  @Nullable
  private static String nameWithPath(SpanData span) {
    String method = span.getAttributes().get(HTTP_REQUEST_METHOD);
    String path = span.getAttributes().get(URL_PATH);
    if (method == null || path == null) {
      return null;
    }
    String name = span.getName();
    // instrumentation populates either method or HTTP when route isn't known.
    if (!name.equals(method) && !name.equals(UNKNOWN_METHOD_SPAN_NAME)) {
      return null;
    }
    return name + " " + path;
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public String toString() {
    return "SpanNamingSpanExporter{" + delegate + "}";
  }

  private static final class RenamedSpanData extends DelegatingSpanData {
    private final String name;

    RenamedSpanData(SpanData delegate, String name) {
      super(delegate);
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
