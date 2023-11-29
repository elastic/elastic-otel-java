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
package co.elastic.otel.config.types;

import co.elastic.otel.config.ConfigurationOption;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UrlConfigurationOption extends ConfigurationOption<URL> {

  protected UrlConfigurationOption(
      boolean dynamic,
      boolean sensitive,
      String key,
      String label,
      String description,
      URL defaultValue,
      String configurationCategory,
      List<String> tags,
      boolean required,
      List<ChangeListener<URL>> changeListeners,
      List<Validator<URL>> validators,
      List<String> aliasKeys,
      Map<String, String> validOptions,
      ChangesFromV1Definitions changesFromV1Definitions) {
    super(
        dynamic,
        sensitive,
        key,
        label,
        description,
        defaultValue,
        configurationCategory,
        null,
        null,
        tags,
        required,
        changeListeners,
        validators,
        aliasKeys,
        validOptions,
        changesFromV1Definitions);
  }

  @Override
  public URL convert(String s) throws IllegalArgumentException {
    try {
      return new URL(removeTrailingSlash(s));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  public static String removeTrailingSlash(String url) {
    if (url != null && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }

  public String toSafeString(URL value) {
    if (value == null) {
      return null;
    }
    final String userInfo = value.getUserInfo();
    final String urlAsString = value.toString();
    if (userInfo != null) {
      return urlAsString.replace(userInfo, getSafeUserInfo(userInfo));
    } else {
      return urlAsString;
    }
  }

  private String getSafeUserInfo(String userInfo) {
    return userInfo.split(":", 2)[0] + ":XXX";
  }

  public static class UrlConfigurationOptionBuilder extends ConfigurationOptionBuilder<URL> {

    public UrlConfigurationOption build() {
      return new UrlConfigurationOption(
          dynamic,
          sensitive,
          key,
          label,
          description,
          defaultValue,
          configurationCategory,
          tags == null ? new ArrayList<>() : Arrays.asList(tags),
          required,
          changeListeners,
          validators,
          aliasKeys == null ? new ArrayList<>() : Arrays.asList(aliasKeys),
          validOptions,
          changesFromV1Definitions);
    }
  }
}
