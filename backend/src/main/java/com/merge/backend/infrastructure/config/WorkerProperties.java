package com.merge.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-queue worker concurrency settings bound from application.properties.
 *
 * Keys match the kebab-case suffixes of {@code job.worker.concurrency.*}
 * and map to the Redis queue keys via {@link com.merge.backend.infrastructure.queue.QueueNames#BY_PROPERTY_KEY}.
 *
 * Example:
 *   job.worker.concurrency.github-commit=3
 *   job.worker.concurrency.build-prd-generation=1
 */
@Component
@ConfigurationProperties(prefix = "job.worker")
public class WorkerProperties {

    private Map<String, Integer> concurrency = new HashMap<>();

    public Map<String, Integer> getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Map<String, Integer> concurrency) {
        this.concurrency = concurrency;
    }

    public int concurrencyFor(String propertyKey) {
        return concurrency.getOrDefault(propertyKey, 1);
    }
}
