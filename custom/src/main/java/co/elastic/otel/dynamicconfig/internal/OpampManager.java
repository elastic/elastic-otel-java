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
package co.elastic.otel.dynamicconfig.internal;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.MapConverter;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.opamp.client.internal.connectivity.http.OkHttpSender;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.delay.RetryPeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.service.HttpRequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.RemoteConfigStatuses;
import opamp.proto.ServerErrorResponse;

public final class OpampManager implements Closeable, OpampClient.Callbacks {
  private final Configuration configuration;
  private final DslJson<Object> dslJson = new DslJson<>(new DslJson.Settings<>());
  private final Logger logger = Logger.getLogger(OpampManager.class.getName());
  private volatile OpampClient client;
  private volatile MutablePeriodicDelay pollingDelay;
  private volatile CentralConfigurationProcessor processor;

  private OpampManager(Configuration configuration) {
    this.configuration = configuration;
  }

  public void start(CentralConfigurationProcessor processor) {
    this.processor = processor;
    pollingDelay = new MutablePeriodicDelay(configuration.pollingInterval);

    OpampClientBuilder builder = OpampClient.builder();
    builder.enableRemoteConfig();

    OkHttpClient.Builder okHttpClient = new OkHttpClient().newBuilder();

    // TODO: revisit this later once the upstream opamp client provides a simpler way to add headers
    okHttpClient
        .interceptors()
        .add(
            chain -> {
              Request.Builder modifiedRequest = chain.request().newBuilder();
              configuration.headers.forEach(modifiedRequest::addHeader);
              return chain.proceed(modifiedRequest.build());
            });

    OkHttpSender httpSender = OkHttpSender.create(configuration.endpointUrl, okHttpClient.build());
    if (configuration.serviceName != null) {
      builder.putIdentifyingAttribute("service.name", configuration.serviceName);
    }
    if (configuration.environment != null) {
      builder.putIdentifyingAttribute("deployment.environment.name", configuration.environment);
    }
    PeriodicDelay retryDelay = RetryPeriodicDelay.create(configuration.pollingInterval);
    builder.setRequestService(HttpRequestService.create(httpSender, pollingDelay, retryDelay));
    client = builder.build(this);
  }

  @Override
  public void close() throws IOException {
    client.close();
  }

  @Override
  public void onMessage(MessageData messageData) {
    logger.log(Level.FINE, "onMessage({0}, {1})", new Object[] {client, messageData});
    AgentRemoteConfig remoteConfig = messageData.getRemoteConfig();
    if (remoteConfig != null) {
      processRemoteConfig(client, remoteConfig);
    }
  }

  private void processRemoteConfig(OpampClient client, AgentRemoteConfig remoteConfig) {
    Map<String, AgentConfigFile> configMapMap = remoteConfig.config.config_map;
    AgentConfigFile centralConfig = configMapMap.get("elastic");
    if (centralConfig != null) {
      Map<String, String> configuration = parseCentralConfiguration(centralConfig.body);
      RemoteConfigStatuses status;

      if (configuration != null) {
        CentralConfigurationProcessor.Result result = processor.process(configuration);
        status =
            (result == CentralConfigurationProcessor.Result.SUCCESS)
                ? RemoteConfigStatuses.RemoteConfigStatuses_APPLIED
                : RemoteConfigStatuses.RemoteConfigStatuses_FAILED;
      } else {
        status = RemoteConfigStatuses.RemoteConfigStatuses_FAILED;
      }

      // Note if FAILED is sent, the config change is effectively dropped as the server will not
      // re-send it
      client.setRemoteConfigStatus(getRemoteConfigStatus(status, remoteConfig.config_hash));
    }
  }

  private static RemoteConfigStatus getRemoteConfigStatus(
      RemoteConfigStatuses status, ByteString hash) {
    if (hash != null && status == RemoteConfigStatuses.RemoteConfigStatuses_APPLIED) {
      return new RemoteConfigStatus.Builder().status(status).last_remote_config_hash(hash).build();
    } else {
      return new RemoteConfigStatus.Builder().status(status).build();
    }
  }

  private Map<String, String> parseCentralConfiguration(ByteString centralConfig) {
    try {
      byte[] centralConfigBytes = centralConfig.toByteArray();
      if (centralConfigBytes.length == 0) {
        logger.log(
            Level.WARNING,
            "No central configuration returned - is this connected to an EDOT collector above 18.8?");
        return null;
      }
      JsonReader<Object> reader = dslJson.newReader(centralConfig.toByteArray());
      reader.startObject();
      return Collections.unmodifiableMap(MapConverter.deserialize(reader));
    } catch (IOException e) {
      logger.log(Level.WARNING, "Could not parse central configuration.", e);
      return null;
    }
  }

  @Override
  public void onConnect() {
    logger.log(Level.INFO, "onConnect({0})", client);
  }

  @Override
  public void onConnectFailed(@Nullable Throwable throwable) {
    logger.log(Level.INFO, "onConnect({0}, {1})", new Object[] {client, throwable});
  }

  @Override
  public void onErrorResponse(@Nonnull ServerErrorResponse errorResponse) {
    logger.log(Level.INFO, "onErrorResponse({0}, {1})", new Object[] {client, errorResponse});
  }

  public void setPollingDelay(@Nonnull Duration duration) {
    pollingDelay.setDelay(duration);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String serviceName;
    @Nullable private String environment;
    private String endpointUrl = "http://localhost:4320/v1/opamp";
    private Duration pollingInterval = Duration.ofSeconds(30);
    private Map<String, String> headers = Collections.emptyMap();

    private Builder() {}

    public Builder setServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder setEndpointHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public Builder setEndpointUrl(String endpointUrl) {
      this.endpointUrl = endpointUrl;
      return this;
    }

    public Builder setPollingInterval(Duration pollingInterval) {
      this.pollingInterval = pollingInterval;
      return this;
    }

    public Builder setServiceEnvironment(@Nullable String environment) {
      this.environment = environment;
      return this;
    }

    public OpampManager build() {
      return new OpampManager(
          new Configuration(serviceName, environment, endpointUrl, pollingInterval, headers));
    }
  }

  public interface CentralConfigurationProcessor {

    Result process(Map<String, String> configuration);

    enum Result {
      SUCCESS,
      FAILURE
    }
  }

  private static class Configuration {
    private final String serviceName;
    @Nullable private final String environment;
    private final String endpointUrl;
    private final Duration pollingInterval;
    private final Map<String, String> headers;

    private Configuration(
        String serviceName,
        @Nullable String environment,
        String endpointUrl,
        Duration pollingInterval,
        Map<String, String> headers) {
      this.serviceName = serviceName;
      this.environment = environment;
      this.endpointUrl = endpointUrl;
      this.pollingInterval = pollingInterval;
      this.headers = headers;
    }
  }

  private static class MutablePeriodicDelay implements PeriodicDelay {
    private final AtomicReference<Duration> duration = new AtomicReference<>();

    public MutablePeriodicDelay(@Nonnull Duration initialValue) {
      duration.set(initialValue);
    }

    @Override
    public Duration getNextDelay() {
      return duration.get();
    }

    @Override
    public void reset() {}

    public void setDelay(@Nonnull Duration value) {
      duration.set(value);
    }
  }
}
