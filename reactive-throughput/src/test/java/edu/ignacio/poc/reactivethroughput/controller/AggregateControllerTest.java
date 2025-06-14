package edu.ignacio.poc.reactivethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
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
@DisplayName("Aggregate Controller Test")
class AggregateControllerTest {

  private static final String AGGREGATE_URL = "/aggregate";

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

  private static String jsonBody(final int id, final String value) {
    return """
      {"id":"%s","itemId":"%d","value":"%s","timestamp":"%s"}
      """.formatted(UUID.randomUUID(), id, value, Instant.now()).strip();
  }

  @Test
  @DisplayName("Should return OK aggregating 3 parallel downstream calls via Flux.merge")
  void shouldReturnOkAggregatingParallelCalls() {
    // Given
    this.stubDownstreamForAllIds("data");

    // When, Then
    this.webTestClient.get().uri(AGGREGATE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(String.class)
      .value(body -> assertThat(body).contains("OK:Reactive:Aggregate:"));
  }

  @Test
  @DisplayName("Should call downstream 3 times and combine results reactively")
  void shouldCallDownstream3TimesAndCombineResultsReactively() {
    // Given
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/1"))
      .willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(jsonBody(1, "alpha"))));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/2"))
      .willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(jsonBody(2, "beta"))));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/3"))
      .willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .withBody(jsonBody(3, "gamma"))));

    // When
    final var result = this.webTestClient.get().uri(AGGREGATE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .returnResult();

    // Then
    assertThat(result.getResponseBody())
      .contains("alpha")
      .contains("beta")
      .contains("gamma");
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/1")));
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/2")));
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/3")));
  }

  @Test
  @DisplayName("Should complete via reactive pipeline without blocking event loop")
  void shouldCompleteViaReactivePipeline() {
    // Given
    this.stubDownstreamForAllIds("item");

    // When, Then
    this.webTestClient.get().uri(AGGREGATE_URL)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .next()
      .as(StepVerifier::create)
      .expectNextMatches(body -> body.contains("OK:Reactive:Aggregate:"))
      .expectComplete()
      .verify(Duration.ofSeconds(5));
  }

  private void stubDownstreamForAllIds(final String valuePrefix) {
    for (int i = 1; i <= 3; i++) {
      wireMock.stubFor(WireMock.get(urlMatching("/api/data/" + i))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
          .withBody(jsonBody(i, valuePrefix + i))));
    }
  }
}
