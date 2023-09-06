package io.opentelemetry.sdk.trace;

public class SdkSpanAccessor {

    public static long getStartEpochNanos(ReadableSpan span) {
        if (span instanceof SdkSpan) {
            // use package-private access when we can to avoid creating single use span data
            return ((SdkSpan) span).getStartEpochNanos();
        } else {
            return span.toSpanData().getStartEpochNanos();
        }
    }
}
