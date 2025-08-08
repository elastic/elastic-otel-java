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

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Collection;
import javax.annotation.Nonnull;

public class BlockableLogRecordExporter implements LogRecordExporter {
  private static volatile BlockableLogRecordExporter INSTANCE;

  private volatile boolean sendingLogs = true;
  private final LogRecordExporter delegate;

  public static BlockableLogRecordExporter getInstance() {
    return INSTANCE;
  }

  public static BlockableLogRecordExporter createCustomInstance(LogRecordExporter exporter) {
    INSTANCE = new BlockableLogRecordExporter(exporter);
    return INSTANCE;
  }

  private BlockableLogRecordExporter(LogRecordExporter delegate) {
    this.delegate = delegate;
  }

  public void setSendingLogs(boolean send) {
    sendingLogs = send;
  }

  public boolean sendingLogs() {
    return sendingLogs;
  }

  @Override
  public CompletableResultCode export(@Nonnull Collection<LogRecordData> collection) {
    if (sendingLogs) {
      return delegate.export(collection);
    } else {
      return CompletableResultCode.ofSuccess();
    }
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
  public void close() {
    delegate.close();
  }

  @Override
  public String toString() {
    return "BlockableLogRecordExporter{"
        + "sendingLogs="
        + sendingLogs
        + ", delegate="
        + delegate
        + '}';
  }
}
