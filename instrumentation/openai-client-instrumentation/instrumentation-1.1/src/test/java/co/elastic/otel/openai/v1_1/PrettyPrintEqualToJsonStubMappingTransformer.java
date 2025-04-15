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
package co.elastic.otel.openai.v1_1;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.StubMappingTransformer;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.List;

public class PrettyPrintEqualToJsonStubMappingTransformer extends StubMappingTransformer {
  @Override
  public String getName() {
    return "pretty-print-equal-to-json";
  }

  @Override
  public StubMapping transform(StubMapping stubMapping, FileSource files, Parameters parameters) {
    List<ContentPattern<?>> patterns = stubMapping.getRequest().getBodyPatterns();
    if (patterns != null) {
      for (int i = 0; i < patterns.size(); i++) {
        ContentPattern<?> pattern = patterns.get(i);
        if (!(pattern instanceof EqualToJsonPattern)) {
          continue;
        }
        EqualToJsonPattern equalToJsonPattern = (EqualToJsonPattern) pattern;
        patterns.set(
            i,
            new EqualToJsonPattern(
                equalToJsonPattern.getExpected(), // pretty printed,
                // We exact match the request unlike the default.
                false,
                false));
      }
    }
    return stubMapping;
  }
}
