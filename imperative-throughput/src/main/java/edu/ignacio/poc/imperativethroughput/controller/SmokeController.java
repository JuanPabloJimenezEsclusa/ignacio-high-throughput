package edu.ignacio.poc.imperativethroughput.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Smoke controller.
 */
@RestController
class SmokeController extends AbstractImperativeController {

  private static final Logger log = LoggerFactory.getLogger(SmokeController.class);
  private static final ScheduledExecutorService scheduler = Executors
    .newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

  SmokeController(final MeterRegistry meterRegistry) {
    super(meterRegistry, "smoke");
  }

  /**
   * Gets smoke.
   *
   * @return the smoke
   */
  @GetMapping({"/smokes", "/smokes/"})
  public CompletableFuture<ResponseEntity<String>> getSmoke() {
    final var future = new CompletableFuture<ResponseEntity<String>>();

    scheduler.schedule(() -> {
      try {
        final Thread currentThread = Thread.currentThread();
        final var response = this.okResponse()
          .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON)))
          .body("OK:Imperative:%s".formatted(currentThread.toString()));
        final var statusCode = response.getStatusCode();

        log.info("Smoke imperative endpoint - status: {} - thread: {}", statusCode, currentThread);
        future.complete(response);
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
    }, 300L, TimeUnit.MILLISECONDS);

    return future;
  }
}
