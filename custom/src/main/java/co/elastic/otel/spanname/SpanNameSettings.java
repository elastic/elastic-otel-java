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

import co.elastic.otel.common.config.WildcardMatcher;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpanNameSettings {

  public static final String USE_PATH_AS_SPAN_NAME = "elastic.otel.use.path.as.span.name";
  public static final String SPAN_NAME_GROUPS = "elastic.otel.span.name.groups";

  private static final String CASE_SENSITIVE_PREFIX = "(?-i)";
  private static final String CASE_INSENSITIVE_PREFIX = "(?i)";

  private static final SpanNameSettings INSTANCE = new SpanNameSettings();

  private volatile boolean usePath;
  private volatile List<NameGroup> groups = Collections.emptyList();

  public static SpanNameSettings getInstance() {
    return INSTANCE;
  }

  // package private for testing
  SpanNameSettings() {}

  public void init(ConfigProperties properties) {
    usePath = properties.getBoolean(USE_PATH_AS_SPAN_NAME, false);
    groups = parseGroups(properties.getList(SPAN_NAME_GROUPS));
  }

  public boolean usePathAsSpanName() {
    return usePath;
  }

  List<NameGroup> spanNameGroups() {
    return groups;
  }

  public void setUsePathAsSpanName(boolean usePath) {
    this.usePath = usePath;
  }

  public void setSpanNameGroups(String groups) {
    List<String> patterns = new ArrayList<>();
    for (String pattern : groups.split(",")) {
      patterns.add(pattern.trim());
    }
    this.groups = parseGroups(patterns);
  }

  private static List<NameGroup> parseGroups(List<String> patterns) {
    List<NameGroup> groups = new ArrayList<>(patterns.size());
    for (String pattern : patterns) {
      if (!pattern.isEmpty()) {
        groups.add(new NameGroup(pattern));
      }
    }
    return Collections.unmodifiableList(groups);
  }

  static final class NameGroup {
    final WildcardMatcher matcher;
    final String name;

    NameGroup(String pattern) {
      this.matcher = WildcardMatcher.valueOf(pattern);
      this.name = stripCasePrefix(pattern);
    }

    private static String stripCasePrefix(String pattern) {
      if (pattern.startsWith(CASE_SENSITIVE_PREFIX)) {
        return pattern.substring(CASE_SENSITIVE_PREFIX.length());
      }
      if (pattern.startsWith(CASE_INSENSITIVE_PREFIX)) {
        return pattern.substring(CASE_INSENSITIVE_PREFIX.length());
      }
      return pattern;
    }
  }
}
