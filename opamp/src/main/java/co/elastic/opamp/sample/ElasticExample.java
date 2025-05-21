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
package co.elastic.opamp.sample;

import co.elastic.opamp.client.CentralConfigurationManager;
import co.elastic.opamp.client.CentralConfigurationProcessor;
import java.time.Duration;
import java.util.logging.Logger;

public class ElasticExample {
  private static final Logger logger = Logger.getLogger(ElasticExample.class.getName());

  public static void main(String[] args) {
    String serviceName = "AsyncSiteChecker";
    if (args.length > 0) {
      serviceName = args[0];
    }
    logger.info("============= Starting client for: " + serviceName);
    CentralConfigurationManager centralConfigurationManager =
        CentralConfigurationManager.builder()
            .setServiceName(serviceName)
            .setServiceVersion("1.0.0")
            .setPollingInterval(Duration.ofSeconds(30))
            .build();

    centralConfigurationManager.start(
        configuration -> {
          logger.info("Received configuration: " + configuration);
          return CentralConfigurationProcessor.Result.SUCCESS;
        });

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("=========== Shutting down");
                  centralConfigurationManager.stop();
                }));
  }
}
