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
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;

public class ResponseHeaderScrubber extends ResponseTransformer {
  @Override
  public String getName() {
    return "scrub-response-header";
  }

  @Override
  public Response transform(
      Request request, Response response, FileSource fileSource, Parameters parameters) {
    HttpHeaders scrubbed = HttpHeaders.noHeaders();
    for (HttpHeader header : response.getHeaders().all()) {
      switch (header.key()) {
        case "Set-Cookie":
          scrubbed = scrubbed.plus(HttpHeader.httpHeader("Set-Cookie", "test_set_cookie"));
          break;
        case "openai-organization":
          scrubbed =
              scrubbed.plus(HttpHeader.httpHeader("openai-organization", "test_openai_org_id"));
          break;
        default:
          scrubbed = scrubbed.plus(header);
          break;
      }
    }
    return Response.Builder.like(response).but().headers(scrubbed).build();
  }
}
