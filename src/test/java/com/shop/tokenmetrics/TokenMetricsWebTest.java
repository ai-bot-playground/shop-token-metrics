package com.shop.tokenmetrics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end web test against a real embedded server. Avoids MockMvc/test
 * autoconfigure imports (relocated in Boot 4) by using the JDK HttpClient and
 * reading the random port from the Environment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenMetricsWebTest {

    @Autowired
    Environment env;

    private final HttpClient http = HttpClient.newHttpClient();

    private String base() {
        return "http://localhost:" + env.getProperty("local.server.port");
    }

    @Test
    void ingestsUsageAndExposesPrometheus() throws Exception {
        String body = "{\"model\":\"z-ai/glm-5.2\",\"source\":\"shop-qa-ui\","
                + "\"promptTokens\":100,\"completionTokens\":200,\"reasoningTokens\":50,\"costUsd\":0.01}";

        HttpResponse<String> post = http.send(
                HttpRequest.newBuilder(URI.create(base() + "/api/usage"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(post.statusCode()).isEqualTo(202);

        HttpResponse<String> scrape = http.send(
                HttpRequest.newBuilder(URI.create(base() + "/actuator/prometheus")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(scrape.statusCode()).isEqualTo(200);
        assertThat(scrape.body()).contains("llm_tokens_total");
        assertThat(scrape.body()).contains("llm_requests_total");
    }
}
