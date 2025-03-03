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
package co.elastic.otel.hostid;

import co.elastic.otel.UniversalProfilingIncubatingAttributes;
import co.elastic.otel.common.WeakConcurrent;
import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import io.opentelemetry.sdk.resources.Resource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfilerHostIdResourceUpdater {

  private static final Logger log = Logger.getLogger(ProfilerHostIdResourceUpdater.class.getName());

  private static volatile HostIdApplicationCache applierCache;

  public static Resource applyHostId(Resource resource) {
    String profilerProvidedHostId = ProfilerProvidedHostId.get();
    if (profilerProvidedHostId == null) {
      return resource;
    }

    HostIdApplicationCache applier = applierCache;
    if (applier == null || !applier.profilerProvidedHostId.equals(profilerProvidedHostId)) {
      applier = new HostIdApplicationCache(profilerProvidedHostId);
      applierCache = applier;
    }
    return applierCache.applyHostId(resource);
  }

  private static class HostIdApplicationCache {
    private final String profilerProvidedHostId;
    private final WeakConcurrentMap<Resource, Resource> cachedUpdates = WeakConcurrent.createMap();

    private HostIdApplicationCache(String profilerProvidedHostId) {
      this.profilerProvidedHostId = profilerProvidedHostId;
    }

    public Resource applyHostId(Resource resource) {
      Resource cached = cachedUpdates.get(resource);
      if (cached == null) {
        cached = doApplyHostId(resource);
        cachedUpdates.putIfAbsent(resource, cached);
        cached = cachedUpdates.get(resource);
      }
      return cached;
    }

    private Resource doApplyHostId(Resource resource) {
      String existingId = resource.getAttribute(UniversalProfilingIncubatingAttributes.HOST_ID);
      if (existingId == null) {
        return resource.merge(
            Resource.builder()
                .put(UniversalProfilingIncubatingAttributes.HOST_ID, profilerProvidedHostId)
                .build());
      } else {
        if (!profilerProvidedHostId.equals(existingId)) {
          log.log(
              Level.WARNING,
              "The used host.id resource attribute ( {0} ) differs from the host.id used for profiling data ( {1} )."
                  + " This will result in the correlation not working correctly!",
              new Object[] {existingId, profilerProvidedHostId});
        }
        return resource;
      }
    }
  }
}
