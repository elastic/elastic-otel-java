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
package co.elastic.otel.dynamicconfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DynamicConfigurationPropertyChecker implements Runnable {
  private static Thread checkerThread;
  private static long interval = 1000;
  private static final Logger logger =
      Logger.getLogger(DynamicConfigurationPropertyChecker.class.getName());

  public static synchronized void startCheckerThread() {
    if (checkerThread != null) {
      return;
    }
    if ("true"
            .equals(
                System.getProperty(
                    DynamicConfiguration.INSTRUMENTATION_DISABLE_OPTION + ".checker"))
        || "true"
            .equals(
                System.getenv("ELASTIC_OTEL_JAVA_EXPERIMENTAL_DISABLE_INSTRUMENTATIONS_CHECKER"))) {
      try {
        interval =
            Long.parseLong(
                System.getenv(
                    "ELASTIC_OTEL_JAVA_EXPERIMENTAL_DISABLE_INSTRUMENTATIONS_CHECKER_INTERVAL_MS"));
      } catch (NumberFormatException e) {
        // do nothing leave the default
      }
      checkerThread =
          new Thread(
              new DynamicConfigurationPropertyChecker(), "Elastic dynamic_instrumentation checker");
      checkerThread.setDaemon(true);
      checkerThread.start();
    }
  }

  private void checkSending() {
    boolean stopSending;
    synchronized (this) {
      stopSending =
          Boolean.parseBoolean(System.getProperty(DynamicConfiguration.DISABLE_SEND_OPTION));
    }
    if (stopSending) {
      DynamicConfiguration.getInstance().stopAllSending();
    } else {
      DynamicConfiguration.getInstance().restartAllSending();
    }
  }

  // Note that if the property and the API are both used to specify enablement
  // for a particular instrument, and this thread is executing, the property
  // will take priority if the instrument is in the property - by virtue of running
  // more frequently; but won't if the instrument is removed from the property!
  // TODO define priority of enablement by source of disabler
  private void checkDisablingInstrumentations() {
    String disableList;
    synchronized (this) {
      disableList = System.getProperty(DynamicConfiguration.INSTRUMENTATION_DISABLE_OPTION);
    }
    DynamicConfiguration.getInstance().deactivateInstrumentations(disableList);
  }

  @Override
  public void run() {
    while (true) {
      try {
        checkSending();
        checkDisablingInstrumentations();
        Thread.sleep(interval);
      } catch (Exception logged) {
        logger.log(Level.SEVERE, "Checker thread hit an exception: ", logged);
      }
    }
  }
}
