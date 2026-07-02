package com.merge.backend.engagement.domain;

public enum MomentumState {
    /** 3+ sessions AND drill pass rate >= 90% */
    DEPLOYING,
    /** 3+ sessions AND drill pass rate < 90% */
    BUILDING,
    /** 1-2 sessions, any outcome */
    COMPILING,
    /** 0 sessions this week, but had at least one session last week */
    BLOCKED,
    /** 0 sessions this week AND 0 sessions last week */
    OFFLINE
}
