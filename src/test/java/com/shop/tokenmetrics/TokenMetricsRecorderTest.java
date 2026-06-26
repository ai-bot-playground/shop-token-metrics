package com.shop.tokenmetrics;

import com.shop.tokenmetrics.api.UsageRequest;
import com.shop.tokenmetrics.service.TokenMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit test of the counter logic — no Spring context required. */
class TokenMetricsRecorderTest {

    @Test
    void recordsTokensRequestsAndCostWithTags() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        TokenMetricsRecorder recorder = new TokenMetricsRecorder(reg);

        recorder.record(new UsageRequest("z-ai/glm-5.2", "shop-qa-ui", 100L, 200L, 50L, 350L, 0.01));
        recorder.record(new UsageRequest("z-ai/glm-5.2", "shop-qa-ui", 10L, 20L, 5L, 35L, 0.002));

        double prompt = reg.get("llm.tokens")
                .tag("type", "prompt").tag("model", "z-ai/glm-5.2").tag("source", "shop-qa-ui")
                .counter().count();
        double completion = reg.get("llm.tokens").tag("type", "completion").counter().count();
        double reasoning = reg.get("llm.tokens").tag("type", "reasoning").counter().count();
        double requests = reg.get("llm.requests").counter().count();
        double cost = reg.get("llm.cost.usd").counter().count();

        assertThat(prompt).isEqualTo(110.0);
        assertThat(completion).isEqualTo(220.0);
        assertThat(reasoning).isEqualTo(55.0);
        assertThat(requests).isEqualTo(2.0);
        assertThat(cost).isEqualTo(0.012);
    }

    @Test
    void omitsMissingCountersAndDefaultsBlankTags() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        TokenMetricsRecorder recorder = new TokenMetricsRecorder(reg);

        // Only prompt tokens; null model/source/cost must not blow up.
        recorder.record(new UsageRequest(null, null, 7L, null, null, null, null));

        double prompt = reg.get("llm.tokens")
                .tag("type", "prompt").tag("model", "unknown").tag("source", "unknown")
                .counter().count();
        assertThat(prompt).isEqualTo(7.0);
        // No cost counter should have been created.
        assertThat(reg.find("llm.cost.usd").counter()).isNull();
    }
}
