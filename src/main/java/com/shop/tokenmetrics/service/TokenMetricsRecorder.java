package com.shop.tokenmetrics.service;

import com.shop.tokenmetrics.api.UsageRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

/**
 * Records LLM token usage into Micrometer counters. Prometheus scrapes the
 * resulting time series from /actuator/prometheus, Grafana charts them.
 *
 * Counters (Prometheus names in parentheses):
 *   llm.tokens   (llm_tokens_total)      tags: type=prompt|completion|reasoning, model, source
 *   llm.requests (llm_requests_total)    tags: model, source
 *   llm.cost.usd (llm_cost_usd_total)    tags: model, source
 *
 * Counters are monotonic; Grafana derives rates / totals from them. Micrometer
 * caches a meter per unique name+tags, so repeated lookups return the same one.
 */
@Service
public class TokenMetricsRecorder {

    private final MeterRegistry registry;

    public TokenMetricsRecorder(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(UsageRequest u) {
        String model = orUnknown(u.model());
        String source = orUnknown(u.source());

        registry.counter("llm.requests", "model", model, "source", source).increment();

        recordTokens("prompt", model, source, u.promptTokens());
        recordTokens("completion", model, source, u.completionTokens());
        recordTokens("reasoning", model, source, u.reasoningTokens());

        if (u.costUsd() != null && u.costUsd() > 0) {
            registry.counter("llm.cost.usd", "model", model, "source", source)
                    .increment(u.costUsd());
        }
    }

    private void recordTokens(String type, String model, String source, Long n) {
        if (n != null && n > 0) {
            registry.counter("llm.tokens", "type", type, "model", model, "source", source)
                    .increment(n);
        }
    }

    private static String orUnknown(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s;
    }
}
