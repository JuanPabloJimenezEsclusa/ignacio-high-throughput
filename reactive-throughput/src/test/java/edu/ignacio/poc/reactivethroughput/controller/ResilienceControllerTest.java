package edu.ignacio.poc.reactivethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Resilience Controller Test")
class ResilienceControllerTest {

  private static final String RESILIENCE_URL = "/resilience";

  @RegisterExtension
  private static final WireMockExtension wireMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @Autowired
  private WebTestClient webTestClient;

  @DynamicPropertySource
  private static void configureDownstreamUrl(final DynamicPropertyRegistry registry) {
    registry.add("downstream.service.url", wireMock::baseUrl);
  }

  @BeforeEach
  void resetStubs() {
    wireMock.resetAll();
  }

  private static String dynamicDownstreamBody() {
    return """
      {
        "id": "%s",
        "value": "downstream-data-%s",
        "timestamp": "%s"
      }
      """.formatted(
      UUID.randomUUID(),
      UUID.randomUUID().toString().substring(0, 8),
      Instant.now().toString());
  }

  @Test
  @DisplayName("Should return OK with downstream data when upstream responds in time")
  void shouldReturnOkWhenDownstreamRespondsInTime() {
    // Given
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(dynamicDownstreamBody())));

    // When, Then
    this.webTestClient.get().uri(RESILIENCE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Map.class)
      .value(raw -> {
        @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) raw;
        assertThat(body)
          .containsEntry("status", "OK")
          .containsEntry("module", "reactive")
          .containsEntry("endpoint", "resilience")
          .containsKey("downstream")
          .containsKey("thread");
      });
  }

  @Test
  @DisplayName("Should return fallback when downstream exceeds timeout")
  void shouldReturnFallbackOnTimeout() {
    // Given - simulate a slow downstream (1000ms > 500ms timeout)
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withFixedDelay(1_000)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(dynamicDownstreamBody())));

    // When, Then - should return fallback, not an error
    this.webTestClient.mutate()
      .responseTimeout(Duration.ofSeconds(5))
      .build()
      .get().uri(RESILIENCE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(Map.class)
      .value(raw -> {
        @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) raw;
        assertThat(body)
          .containsEntry("status", "FALLBACK")
          .containsEntry("module", "reactive")
          .containsEntry("endpoint", "resilience")
          .containsEntry("reason", "timeout")
          .containsKey("thread");
      });
  }

  @Test
  @DisplayName("Should return fallback when delayMs query param exceeds timeout")
  void shouldReturnFallbackWhenDelayMsExceedsTimeout() {
    // Given - delayMs=1000ms is forwarded to downstream, exceeding the 500ms timeout
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withFixedDelay(1_000)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(dynamicDownstreamBody())));

    // When, Then
    this.webTestClient.mutate()
      .responseTimeout(Duration.ofSeconds(5))
      .build()
      .get().uri(RESILIENCE_URL + "?delayMs=1000")
      .exchange()
      .expectStatus().isOk()
      .expectBody(Map.class)
      .value(raw -> {
        @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) raw;
        assertThat(body)
          .containsEntry("status", "FALLBACK")
          .containsEntry("module", "reactive")
          .containsEntry("endpoint", "resilience")
          .containsEntry("reason", "timeout")
          .containsKey("thread");
      });
  }

  @Test
  @DisplayName("Should complete via reactive pipeline without blocking event loop")
  void shouldCompleteViaReactivePipeline() {
    // Given
    wireMock.stubFor(WireMock.get(urlPathEqualTo("/api/data"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(dynamicDownstreamBody())));

    // When, Then
    this.webTestClient.get().uri(RESILIENCE_URL)
      .exchange()
      .expectStatus().isOk()
      .returnResult(Map.class)
      .getResponseBody()
      .next()
      .as(StepVerifier::create)
      .expectNextMatches(raw -> {
        @SuppressWarnings("unchecked") final Map<String, Object> body = (Map<String, Object>) raw;
        return "OK".equals(body.get("status")) && body.containsKey("downstream");
      })
      .expectComplete()
      .verify(Duration.ofSeconds(5));
  }
}
