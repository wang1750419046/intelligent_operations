package com.example.aiops.agent;

public final class AgentRunContextHolder {

    private static final ThreadLocal<AgentRunContext> CONTEXT = new InheritableThreadLocal<>();

    private AgentRunContextHolder() {
    }

    public static void set(AgentRunContext context) {
        CONTEXT.set(context);
    }

    public static AgentRunContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
