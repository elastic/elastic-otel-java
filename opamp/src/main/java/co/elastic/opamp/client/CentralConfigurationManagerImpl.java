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
package co.elastic.opamp.client;

import co.elastic.opamp.client.connectivity.http.OkHttpSender;
import co.elastic.opamp.client.request.delay.PeriodicDelay;
import co.elastic.opamp.client.request.service.HttpRequestService;
import co.elastic.opamp.client.response.MessageData;
import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.MapConverter;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import opamp.proto.Opamp;

public final class CentralConfigurationManagerImpl
    implements CentralConfigurationManager, OpampClient.Callback {
  private final OpampClient client;
  private final DslJson<Object> dslJson = new DslJson<>(new DslJson.Settings<>());
  private final Logger logger = Logger.getLogger(CentralConfigurationManagerImpl.class.getName());
  private CentralConfigurationProcessor processor;

  CentralConfigurationManagerImpl(OpampClient client) {
    this.client = client;
  }

  @Override
  public void start(CentralConfigurationProcessor processor) {
    this.processor = processor;
    client.start(this);
  }

  @Override
  public void stop() {
    client.stop();
  }

  @Override
  public void onMessage(OpampClient client, MessageData messageData) {
    logger.log(Level.FINE, "onMessage({0}, {1})", new Object[] {client, messageData});
    Opamp.AgentRemoteConfig remoteConfig = messageData.getRemoteConfig();
    if (remoteConfig != null) {
      processRemoteConfig(client, remoteConfig);
    }
  }

  private void processRemoteConfig(OpampClient client, Opamp.AgentRemoteConfig remoteConfig) {
    Map<String, Opamp.AgentConfigFile> configMapMap = remoteConfig.getConfig().getConfigMapMap();
    Opamp.AgentConfigFile centralConfig = configMapMap.get("elastic");
    if (centralConfig != null) {
      Map<String, String> configuration = parseCentralConfiguration(centralConfig.getBody());
      Opamp.RemoteConfigStatuses status;

      if (configuration != null) {
        CentralConfigurationProcessor.Result result = processor.process(configuration);
        status =
            (result == CentralConfigurationProcessor.Result.SUCCESS)
                ? Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLIED
                : Opamp.RemoteConfigStatuses.RemoteConfigStatuses_FAILED;
      } else {
        status = Opamp.RemoteConfigStatuses.RemoteConfigStatuses_FAILED;
      }

      client.setRemoteConfigStatus(getRemoteConfigStatus(status, remoteConfig.getConfigHash()));
    }
  }

  private static Opamp.RemoteConfigStatus getRemoteConfigStatus(
      Opamp.RemoteConfigStatuses status, ByteString hash) {
    if (hash != null && status == Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLIED) {
      return Opamp.RemoteConfigStatus.newBuilder()
          .setStatus(status)
          .setLastRemoteConfigHash(hash)
          .build();
    } else {
      return Opamp.RemoteConfigStatus.newBuilder().setStatus(status).build();
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
  public void onConnect(OpampClient client) {
    logger.log(Level.INFO, "onConnect({0})", client);
  }

  @Override
  public void onConnectFailed(OpampClient client, Throwable throwable, Duration nextTry) {
    if (nextTry == null) {
      logger.log(Level.INFO, "onConnect({0}, {1})", new Object[] {client, throwable});
    } else {
      logger.log(
          Level.INFO,
          "onConnect({0}, {1}, next attempt to connect in {2})",
          new Object[] {client, throwable, nextTry});
    }
  }

  @Override
  public void onErrorResponse(
      OpampClient client, Opamp.ServerErrorResponse errorResponse, Duration nextTry) {
    if (nextTry == null) {
      logger.log(Level.INFO, "onErrorResponse({0}, {1})", new Object[] {client, errorResponse});
    } else {
      logger.log(
          Level.INFO,
          "onErrorResponse({0}, {1}, next attempt to send in {2})",
          new Object[] {client, errorResponse, nextTry});
    }
  }

  public static class Builder {
    private String serviceName;
    private String serviceNamespace;
    private String serviceVersion;
    private String environment;
    private String configurationEndpoint;
    private Duration pollingInterval;

    Builder() {}

    public Builder setServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder setServiceNamespace(String serviceNamespace) {
      this.serviceNamespace = serviceNamespace;
      return this;
    }

    public Builder setServiceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
      return this;
    }

    public Builder setConfigurationEndpoint(String configurationEndpoint) {
      this.configurationEndpoint = configurationEndpoint;
      return this;
    }

    public Builder setPollingInterval(Duration pollingInterval) {
      this.pollingInterval = pollingInterval;
      return this;
    }

    public Builder setServiceEnvironment(String environment) {
      this.environment = environment;
      return this;
    }

    public CentralConfigurationManagerImpl build() {
      OpampClientBuilder builder = OpampClient.builder();
      OkHttpSender httpSender = OkHttpSender.create("http://localhost:4320/v1/opamp");
      PeriodicDelay pollingDelay = HttpRequestService.DEFAULT_DELAY_BETWEEN_REQUESTS;
      PeriodicDelay retryDelay = PeriodicDelay.ofVariableDuration(pollingDelay.getNextDelay());
      if (serviceName != null) {
        builder.setServiceName(serviceName);
      }
      if (serviceNamespace != null) {
        builder.setServiceNamespace(serviceNamespace);
      }
      if (serviceVersion != null) {
        builder.setServiceVersion(serviceVersion);
      }
      if (environment != null) {
        builder.setServiceEnvironment(environment);
      }
      if (configurationEndpoint != null) {
        httpSender = OkHttpSender.create(configurationEndpoint);
      }
      if (pollingInterval != null) {
        pollingDelay = PeriodicDelay.ofFixedDuration(pollingInterval);
        retryDelay = PeriodicDelay.ofVariableDuration(pollingInterval);
      }
      builder.setRequestService(HttpRequestService.create(httpSender, pollingDelay, retryDelay));
      return new CentralConfigurationManagerImpl(builder.build());
    }
  }
}
