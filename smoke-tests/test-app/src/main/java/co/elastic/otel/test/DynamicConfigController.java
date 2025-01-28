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
@RequestMapping("/dynamicconfig")
public class DynamicConfigController {
  public static final String DISABLE_SEND_OPTION = "elastic.otel.java.experimental.disable_send";

  // note synchronized to make enable/disable faster with DynamicInstrumentation
  @GetMapping("/flipSending")
  public synchronized String flipSending() {
    String old = System.getProperty(DISABLE_SEND_OPTION, "");
    String result;
    if (old.isEmpty()) {
      System.setProperty(DISABLE_SEND_OPTION, "true");
      result = "stopped";
    } else {
      System.setProperty(DISABLE_SEND_OPTION, "");
      result = "restarted";
    }
    waitForSystemPropertyChangeEffective();
    return result;
  }

  private static void waitForSystemPropertyChangeEffective() {
    try {
      Thread.sleep(1000L);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @RequestMapping("/reset")
  public synchronized String reset() {
    String old = System.getProperty(DISABLE_SEND_OPTION, "");
    if (!old.isEmpty()) {
      System.setProperty(DISABLE_SEND_OPTION, "");
      waitForSystemPropertyChangeEffective();
    }
    return "reset";
  }
}
