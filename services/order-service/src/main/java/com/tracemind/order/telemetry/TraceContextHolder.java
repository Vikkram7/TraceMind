package com.tracemind.order.telemetry;

public class TraceContextHolder {
    private static final ThreadLocal<TraceContext> CONTEXT = new ThreadLocal<>();

    public static TraceContext get() {
        return CONTEXT.get();
    }

    public static void set(TraceContext context) {
        CONTEXT.set(context);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
