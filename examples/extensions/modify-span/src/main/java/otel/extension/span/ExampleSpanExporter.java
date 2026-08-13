package otel.extension.span;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExampleSpanExporter implements SpanExporter {

  private final SpanExporter delegate;

  public ExampleSpanExporter(SpanExporter delegate) {
    this.delegate = delegate;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {

    List<SpanData> toExport = spans.stream()
        // discarding spans is only possible in span exporter, unlike modifying spans which can
        // be done both in span exporter or span processor
        .filter(ExampleSpanExporter::shouldKeepSpan)
        .collect(Collectors.toList());

    return delegate.export(toExport);
  }

  private static boolean shouldKeepSpan(SpanData span) {
    // this will only discard the top-level HTTP span, not any child span like a database call
    String path = span.getAttributes().get(UrlAttributes.URL_PATH);
    return path == null || !path.startsWith("/healthcheck");
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }
}
