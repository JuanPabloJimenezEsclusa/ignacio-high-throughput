package edu.ignacio.poc.imperativethroughput.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * The type Aggregate controller.
 *
 * <p>Demonstrates parallel fan-out using virtual threads and {@link CompletableFuture#allOf}.
 * Three concurrent downstream calls are issued in parallel — each on its own
 * virtual thread — and their results are combined when all complete. If any subtask
 * fails, the exception propagates immediately via {@link CompletableFuture#join()}.
 * Compare with the reactive counterpart using {@code Flux.merge()} / {@code Mono.zip()}.
 */
@RestController
class AggregateController {

  private static final Logger log = LoggerFactory.getLogger(AggregateController.class);
  private static final String DOWNSTREAM_PATH = "/api/data/{id}";
  private static final int FAN_OUT = 3;

  private final RestClient restClient;
  private final Timer aggregateTimer;

  AggregateController(
    @Value("${downstream.service.url}") final String downstreamUrl,
    final MeterRegistry meterRegistry
  ) {
    this.restClient = RestClient.builder()
      .baseUrl(downstreamUrl)
      .build();
    this.aggregateTimer = Timer.builder("http.request.duration")
      .description("Aggregate endpoint request duration")
      .tag("module", "imperative")
      .tag("endpoint", "aggregate")
      .register(meterRegistry);
  }

  /**
   * Gets aggregate.
   *
   * @return the aggregated result
   */
  @GetMapping({"/aggregate", "/aggregate/"})
  public CompletableFuture<ResponseEntity<String>> getAggregate() {
    final var executor = Executors.newVirtualThreadPerTaskExecutor();
    return CompletableFuture.supplyAsync(this::aggregate, executor);
  }

  private ResponseEntity<String> aggregate() {
    final var sample = Timer.start();
    final var currentThread = Thread.currentThread();

    try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) { // NOPMD
      final List<CompletableFuture<String>> futures = IntStream.rangeClosed(1, FAN_OUT)
        .mapToObj(i -> CompletableFuture.supplyAsync(() -> this.fetchData(i), executor))
        .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      final var combined = futures.stream()
        .map(CompletableFuture::join)
        .reduce((a, b) -> a + "," + b)
        .orElse("");

      log.info("Aggregate imperative endpoint - results: {} - thread: {}", combined, currentThread);
      sample.stop(this.aggregateTimer);

      return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache())
        .contentType(MediaType.APPLICATION_JSON)
        .body("OK:Imperative:Aggregate:[%s]:%s".formatted(combined, currentThread));
    }
  }

  private String fetchData(final int index) {
    return this.restClient.get()
      .uri(DOWNSTREAM_PATH, index)
      .retrieve()
      .body(String.class);
  }
}
