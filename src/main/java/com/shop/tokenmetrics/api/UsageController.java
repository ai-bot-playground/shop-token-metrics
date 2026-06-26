package com.shop.tokenmetrics.api;

import com.shop.tokenmetrics.service.TokenMetricsRecorder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ingest endpoint for LLM token usage. Clients POST one event per completion;
 * the counts land in Micrometer and are exposed at /actuator/prometheus.
 */
@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final TokenMetricsRecorder recorder;

    public UsageController(TokenMetricsRecorder recorder) {
        this.recorder = recorder;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> record(@RequestBody UsageRequest req) {
        recorder.record(req);
        return ResponseEntity.accepted().body(Map.of("status", "recorded"));
    }
}
