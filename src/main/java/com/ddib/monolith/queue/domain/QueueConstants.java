package com.ddib.monolith.queue.domain;

public final class QueueConstants {

    public static final long SCHEDULER_RATE_MS = 1000L;
    public static final long MAX_ACTIVE_USERS = 2000L;
    public static final long MAX_ENTRIES_PER_CYCLE = 100L;
    public static final long ACTIVE_USER_TIMEOUT_MINUTES = 5L;
    public static final String SSE_EVENT_NAME = "queue-status";

    private QueueConstants() {
    }
}

