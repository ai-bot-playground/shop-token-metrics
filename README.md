# shop-token-metrics

Lightweight observability service for **LLM token usage** across the platform.
Clients (today: `shop-qa-ui`) report each LLM call's token accounting; this
service aggregates it into **Micrometer** counters and exposes them on
`/actuator/prometheus`, where **Prometheus** scrapes them and **Grafana** charts
them.

It is intentionally minimal: no database, no Kafka — counters live in-memory and
Prometheus owns the time-series history.

## API

`POST /api/usage` — report one completion's usage (all counters optional):

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

## Metrics (Prometheus names)

| Metric | Type | Tags | Meaning |
|---|---|---|---|
| `llm_tokens_total` | counter | `type` (prompt/completion/reasoning), `model`, `source` | tokens consumed |
| `llm_requests_total` | counter | `model`, `source` | number of LLM calls |
| `llm_cost_usd_total` | counter | `model`, `source` | accumulated USD cost (when the provider reports it) |

Example PromQL:
- Total tokens: `sum(llm_tokens_total)`
- Tokens/min by type: `sum by (type) (rate(llm_tokens_total[5m])) * 60`
- Cost so far: `sum(llm_cost_usd_total)`

## Stack

Spring Boot 4 / Java 25 / Gradle, Spring Boot Actuator + `micrometer-registry-prometheus`.

## Run

```bash
./gradlew bootRun
# POST a sample event
curl -X POST localhost:8080/api/usage -H 'content-type: application/json' \
  -d '{"model":"z-ai/glm-5.2","source":"shop-qa-ui","promptTokens":100,"completionTokens":200,"reasoningTokens":50,"costUsd":0.01}'
# scrape
curl localhost:8080/actuator/prometheus | grep llm_
```

## Deploy

Part of the umbrella Helm chart in `shop-infra/helm`. Prometheus + Grafana are
deployed alongside it (see `shop-infra/helm/templates/infra-prometheus.yaml` and
`infra-grafana.yaml`, gated by `observability.enabled`). The Grafana dashboard
**"LLM Token Usage"** is provisioned automatically.
