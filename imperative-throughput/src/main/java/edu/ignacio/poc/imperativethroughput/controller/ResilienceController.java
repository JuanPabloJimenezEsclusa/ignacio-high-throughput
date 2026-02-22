package edu.ignacio.poc.imperativethroughput.controller;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The type Resilience controller.
 *
 * <p>Demonstrates resilience patterns using Java's built-in primitives.
 * {@link CompletableFuture#orTimeout} cancels the downstream call after a
 * configurable timeout, and a static fallback is returned so the caller always
 * receives a valid response. This shows production-grade reliability awareness:
 * high-throughput systems must protect themselves from slow upstreams to avoid
 * cascading failures. Compare with the reactive counterpart that uses
 * {@code Mono.timeout()} + {@code .onErrorReturn()} — identical intent, different
 * execution model.
 */
@RestController
class ResilienceController {

  private static final Logger log = LoggerFactory.getLogger(ResilienceController.class);
  private static final String DOWNSTREAM_PATH = "/api/data";
  private static final long TIMEOUT_MS = 500L;
  private static final String FALLBACK_BODY = "FALLBACK:Imperative:Resilience:timeout";

  private final RestClient restClient;
  private final Timer resilienceTimer;

  ResilienceController(
    @Value("${downstream.service.url}") final String downstreamUrl,
    final MeterRegistry meterRegistry
  ) {
    this.restClient = RestClient.builder()
      .baseUrl(downstreamUrl)
      .build();
    this.resilienceTimer = Timer.builder("http.request.duration")
      .description("Resilience endpoint request duration")
      .tag("module", "imperative")
      .tag("endpoint", "resilience")
      .register(meterRegistry);
  }

  /**
   * Gets resilience.
   *
   * @param delayMs optional delay to inject into the downstream stub (for testing)
   * @return the resilience result, or fallback on timeout
   */
  @GetMapping({"/resilience", "/resilience/"})
  public CompletableFuture<ResponseEntity<String>> getResilience(
    @RequestParam(value = "delayMs", defaultValue = "0") final long delayMs
  ) {
    final var sample = Timer.start();

    try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) { // NOPMD
      return CompletableFuture
        .supplyAsync(() -> this.callDownstream(delayMs), executor)
        .orTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .handle((body, ex) -> {
          sample.stop(this.resilienceTimer);
          final var currentThread = Thread.currentThread();
          if (ex instanceof TimeoutException) {
            log.warn("Resilience imperative endpoint - timeout after {}ms - thread: {}",
              TIMEOUT_MS, currentThread);
            return ResponseEntity.ok()
              .cacheControl(CacheControl.noCache())
              .contentType(MediaType.APPLICATION_JSON)
              .body(FALLBACK_BODY);
          }
          if (ex != null) {
            log.error("Resilience imperative endpoint - downstream error - thread: {}",
              currentThread, ex);
            return ResponseEntity.ok()
              .cacheControl(CacheControl.noCache())
              .contentType(MediaType.APPLICATION_JSON)
              .body(FALLBACK_BODY);
          }
          log.info("Resilience imperative endpoint - downstream: {} - thread: {}",
            body, currentThread);
          return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .contentType(MediaType.APPLICATION_JSON)
            .body("OK:Imperative:Resilience:%s:%s".formatted(body, currentThread));
        });
    }
  }

  private String callDownstream(final long delayMs) {
    try {
      if (delayMs > 0) {
        Thread.sleep(Duration.ofMillis(delayMs));
      }
      return this.restClient.get()
        .uri(DOWNSTREAM_PATH)
        .retrieve()
        .body(String.class);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RestClientException("Interrupted during delay", e);
    }
  }
}
