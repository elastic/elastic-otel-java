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
package co.elastic.otel.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dynamic")
public class DynamicInstrumentationController {
  public static final String INSTRUMENTATION_DISABLE_OPTION =
      "elastic.otel.java.disable_instrumentations";

  // note synchronized to make enable/disable faster with DynamicInstrumentation
  @GetMapping
  public synchronized String flipMethods() {
    String old = System.getProperty(INSTRUMENTATION_DISABLE_OPTION, "");
    if (old.isEmpty()) {
      System.setProperty(INSTRUMENTATION_DISABLE_OPTION, "methods");
    } else {
      System.setProperty(INSTRUMENTATION_DISABLE_OPTION, "");
    }
    return old.isEmpty() ? "enabled" : "disabled";
  }
}
