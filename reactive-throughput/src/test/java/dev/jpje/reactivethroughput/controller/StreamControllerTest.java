package dev.jpje.reactivethroughput.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Stream Controller Test")
class StreamControllerTest {

  private static final String STREAM_URL = "/stream";
  private static final int EXPECTED_EVENT_COUNT = 10;
  private static final List<Integer> EXPECTED_SEQUENCE = IntStream.rangeClosed(1, EXPECTED_EVENT_COUNT).boxed().toList();

  @Autowired
  private WebTestClient webTestClient;

  @Test
  @DisplayName("Should return text/event-stream content type")
  void shouldReturnEventStreamContentType() {
    this.webTestClient.mutate()
      .responseTimeout(Duration.ofSeconds(15))
      .build()
      .get().uri(STREAM_URL)
      .accept(MediaType.TEXT_EVENT_STREAM)
      .exchange()
      .expectStatus().isOk()
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
  }

  @Test
  @DisplayName("Should emit exactly 10 events and then complete")
  void shouldEmitExactly10EventsAndComplete() {
    // When, Then
    this.webTestClient.mutate()
      .responseTimeout(Duration.ofSeconds(15))
      .build()
      .get().uri(STREAM_URL)
      .accept(MediaType.TEXT_EVENT_STREAM)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .as(flux -> StepVerifier.create(flux)
        .expectNextCount(EXPECTED_EVENT_COUNT)
        .expectComplete()
        .verify(Duration.ofSeconds(15)));
  }

  @Test
  @DisplayName("Should emit events prefixed with 'event:' in sequence")
  void shouldEmitSequentialEventPrefixedWithEventColon() {
    // When
    final var events = this.webTestClient.mutate()
      .responseTimeout(Duration.ofSeconds(15))
      .build()
      .get().uri(STREAM_URL)
      .accept(MediaType.TEXT_EVENT_STREAM)
      .exchange()
      .expectStatus().isOk()
      .returnResult(String.class)
      .getResponseBody()
      .collectList()
      .block(Duration.ofSeconds(15));

    // Then
    assertThat(events)
      .as("events list must not be null and have expected size")
      .isNotNull()
      .hasSize(EXPECTED_EVENT_COUNT)
      .allSatisfy(event -> assertThat(event).as("every event must start with 'event:'").startsWith("event:"));

    final var numbers = events.stream()
      .map(e -> Integer.parseInt(e.split(":")[1]))
      .toList();
    assertThat(numbers).as("event numbers must be sequential 1..%d", EXPECTED_EVENT_COUNT).isEqualTo(EXPECTED_SEQUENCE);
  }
}
