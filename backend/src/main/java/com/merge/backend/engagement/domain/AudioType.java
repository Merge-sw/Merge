package com.merge.backend.engagement.domain;

public enum AudioType {
    /** 5-7 min deep-dive on the student's current concept. Queued when EXHAUSTED mid-concept. */
    REINFORCEMENT,
    /** Intro to the next concept. Queued when EXHAUSTED at a concept boundary. */
    PRIMER
}
