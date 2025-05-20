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
package co.elastic.opamp.client.connectivity.http;

import java.util.Objects;

/**
 * Exception provided inside a co.elastic.opamp.client.request.service.RequestService.Response.Error
 * response from a {@link co.elastic.opamp.client.request.service.RequestService}.
 */
public class HttpErrorException extends Exception {
  public final int errorCode;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HttpErrorException that = (HttpErrorException) o;
    return errorCode == that.errorCode && Objects.equals(getMessage(), that.getMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorCode, getMessage());
  }

  /**
   * Constructs an HTTP error related exception.
   *
   * @param errorCode The HTTP error code.
   * @param message The HTTP error message associated with the code.
   */
  public HttpErrorException(int errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
