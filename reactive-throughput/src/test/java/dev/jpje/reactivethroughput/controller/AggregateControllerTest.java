package dev.jpje.reactivethroughput.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

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
  private static final String OK_PREFIX = "OK:Reactive:Aggregate:";

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

  @Test
  @DisplayName("Should return OK aggregating 3 parallel downstream calls via Flux.mergeSequential")
  void shouldReturnOkAggregatingParallelCalls() {
    // Given
    this.stubDownstreamForAllIds();

    // When, Then
    this.webTestClient.get().uri(AGGREGATE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(String.class)
      .value(body -> assertThat(body).startsWith(OK_PREFIX));

    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/1")));
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/2")));
    wireMock.verify(1, WireMock.getRequestedFor(urlMatching("/api/data/3")));
  }

  @Test
  @DisplayName("Should concatenate raw downstream bodies in id order regardless of completion order")
  void shouldConcatenateBodiesInIdOrder() {
    // Given - id 3 completes first and id 1 completes last, so emission order differs from id order
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/3"))
      .willReturn(aResponse().withStatus(200).withFixedDelay(10).withBody("gamma")));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/2"))
      .willReturn(aResponse().withStatus(200).withFixedDelay(30).withBody("beta")));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/1"))
      .willReturn(aResponse().withStatus(200).withFixedDelay(60).withBody("alpha")));

    // When
    final var result = this.webTestClient.get().uri(AGGREGATE_URL)
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class)
      .returnResult();

    // Then
    assertThat(result.getResponseBody())
      .contains("OK:Reactive:Aggregate:[alpha,beta,gamma]:");
  }

  @Test
  @DisplayName("Should complete via reactive pipeline without blocking event loop")
  void shouldCompleteViaReactivePipeline() {
    // Given
    this.stubDownstreamForAllIds();

    // When, Then
    this.webTestClient.get().uri(AGGREGATE_URL)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .next()
      .as(StepVerifier::create)
      .expectNextMatches(body -> body.startsWith(OK_PREFIX))
      .expectComplete()
      .verify(Duration.ofSeconds(5));
  }

  private void stubDownstreamForAllIds() {
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/1"))
      .willReturn(aResponse().withStatus(200).withBody("alpha")));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/2"))
      .willReturn(aResponse().withStatus(200).withBody("beta")));
    wireMock.stubFor(WireMock.get(urlMatching("/api/data/3"))
      .willReturn(aResponse().withStatus(200).withBody("gamma")));
  }
}
