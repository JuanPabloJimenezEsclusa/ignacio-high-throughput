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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
class AggregateController extends AbstractImperativeController {

  private static final Logger log = LoggerFactory.getLogger(AggregateController.class);
  private static final String API_DATA_ID = "/api/data/{id}";
  private static final int FAN_OUT = 3;

  AggregateController(
    @Value("${downstream.service.url}") final String downstreamUrl,
    final MeterRegistry meterRegistry
  ) {
    super(downstreamUrl, meterRegistry, "aggregate");
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
      sample.stop(this.timer);

      return this.okResponse()
        .body("OK:Imperative:Aggregate:[%s]:%s".formatted(combined, currentThread));
    }
  }

  private String fetchData(final int index) {
    return this.restClient.get()
      .uri(API_DATA_ID, index)
      .retrieve()
      .body(String.class);
  }
}
