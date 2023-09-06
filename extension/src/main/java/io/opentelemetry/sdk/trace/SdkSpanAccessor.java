package io.opentelemetry.sdk.trace;

public class SdkSpanAccessor {

    public static long getStartEpochNanos(ReadableSpan span) {
        // unfortunately we can't cast to SdkSpan as it's loaded in AgentClassloader and the extension is loaded
        // in the ExtensionClassloader, hence we can't use the package-protected SdkSpan#getStartEpochNanos method
        return span.toSpanData().getStartEpochNanos();
    }
}
