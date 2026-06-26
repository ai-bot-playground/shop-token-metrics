package com.shop.tokenmetrics.api;

/**
 * One LLM call's token accounting, reported by a client (e.g. shop-qa-ui) after
 * it receives a completion. All counters are optional — a client may not know
 * every field (e.g. reasoning/cost are only present for some providers).
 */
public record UsageRequest(
        String model,
        String source,
        Long promptTokens,
        Long completionTokens,
        Long reasoningTokens,
        Long totalTokens,
        Double costUsd
) {
}
