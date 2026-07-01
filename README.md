# shop-token-metrics

Lightweight service that accepts LLM token usage reports from clients and exposes them as Prometheus counters via `/actuator/prometheus`. No database — counters are in-memory; Prometheus owns the history.

## API

`POST /api/usage` — all fields optional:

```json
{
  "model": "z-ai/glm-5.2",
  "source": "shop-qa-ui",
  "promptTokens": 1234,
  "completionTokens": 567,
  "reasoningTokens": 102,
  "totalTokens": 1903,
  "costUsd": 0.0041
}
```

→ `202 Accepted` `{"status":"recorded"}`

## Metrics

| Metric | Tags |
|---|---|
| `llm_tokens_total` | `type` (prompt/completion/reasoning), `model`, `source` |
| `llm_requests_total` | `model`, `source` |
| `llm_cost_usd_total` | `model`, `source` |

## Stack & Run

Spring Boot 4.0.7 · Java 25 · Gradle

```bash
./gradlew bootRun

# send a sample event
curl -X POST localhost:8080/api/usage -H 'content-type: application/json' \
  -d '{"model":"z-ai/glm-5.2","source":"shop-qa-ui","promptTokens":100,"completionTokens":200,"costUsd":0.01}'

# verify
curl localhost:8080/actuator/prometheus | grep llm_
```
