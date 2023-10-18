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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiling")
public class ProfilingController {

  private static final Tracer tracer =
      GlobalOpenTelemetry.get().getTracerProvider().tracerBuilder("test-profiling").build();

  @GetMapping("/scenario/{id}")
  public String scenario(@PathVariable("id") int id) {
    switch (id) {
      case 1:
        createSpan(10, sleepBody(5));
        break;
      case 2:
        createRecursiveSpans(20, 7, 1, 1);
        break;
      case 3:
        createRecursiveSpans(30, 7, 1, 0);
        createRecursiveSpans(35, 7, 0, 1);
        break;
      case 4:
        // create span on current thread
        // delegate the span context execution on other threads
        Span span = tracer.spanBuilder("span #" + 40).startSpan();
        Context spanContext = Context.current().with(span);

        TaskWithContext taskWithContext =
            (context, endSpan) -> {
              System.out.printf(
                  "execute task in thread %d, span ID = %s%n",
                  Thread.currentThread().getId(),
                  Span.fromContext(context).getSpanContext().getSpanId());
              try (Scope scope = context.makeCurrent()) {
                sleepBody(1).run();
              } finally {
                if (endSpan) {
                  Span.fromContext(context).end();
                }
              }
            };

        // first "activation" is in another thread
        executeInAnotherThread(taskWithContext, spanContext, false);

        // second "activation" is in current thread
        taskWithContext.run(spanContext, false);

        // last activation and end is in another
        executeInAnotherThread(taskWithContext, spanContext, true);
        break;
      default:
        throw new IllegalArgumentException("unknown test scenario " + id);
    }

    return String.format("scenario %d OK", id);
  }

  private static void executeInAnotherThread(
      TaskWithContext taskWithContext, Context spanContext, boolean endSpan) {
    Thread t =
        new Thread(
            () -> {
              taskWithContext.run(spanContext, endSpan);
            });
    t.start();
    try {
      t.join(5_000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private interface TaskWithContext {
    void run(Context context, boolean endSpan);
  }

  public void createSpan(int id, Runnable taskBody) {
    Span span = tracer.spanBuilder("span #" + id).startSpan();
    try (Scope scope = span.makeCurrent()) {
      System.out.println(String.format("span #%d start", id));
      taskBody.run();
      System.out.println(String.format("span #%d end", id));
    } finally {
      span.end();
    }
  }

  public Runnable sleepBody(int duration) {
    return () -> doSleep(duration);
  }

  public void createRecursiveSpans(int id, int duration, int beforeDelay, int afterDelay) {
    if (duration <= 2) {
      createSpan(id, sleepBody(duration));
    } else {
      createSpan(
          id,
          () -> {
            doSleep(beforeDelay);
            createRecursiveSpans(id + 1, duration - 2, beforeDelay, afterDelay);
            doSleep(afterDelay);
          });
    }
  }

  private static void doSleep(int duration) {
    try {
      Thread.sleep(duration * 100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
