/*
 * Copyright 2014-2019 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.elastic.apm.otel.profiler.collections;

/** This is an (int, int) primitive specialisation of a BiConsumer */
@FunctionalInterface
public interface IntIntConsumer {
  /**
   * Accept two values that comes as a tuple of ints.
   *
   * @param valueOne for the tuple.
   * @param valueTwo for the tuple.
   */
  void accept(int valueOne, int valueTwo);
}
