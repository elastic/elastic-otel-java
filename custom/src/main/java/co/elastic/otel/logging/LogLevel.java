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
package co.elastic.otel.logging;

public enum LogLevel {
  OFF(false),
  ERROR(false),
  CRITICAL(false),
  WARN(false),
  WARNING(false),
  INFO(false),
  DEBUG(true),
  TRACE(true);

  // Otel agent has a debug on/off mode, hence this particular support
  private final boolean isDebug;

  LogLevel(boolean isDebug) {
    this.isDebug = isDebug;
  }

  public boolean isDebug() {
    return isDebug;
  }
}
