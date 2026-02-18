package edu.ignacio.poc.imperativethroughput.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Smoke controller.
 */
@RestController
class SmokeController {

  private static final Logger log = LoggerFactory.getLogger(SmokeController.class);
  private static final ScheduledExecutorService scheduler = Executors
    .newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

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
        final var response = ResponseEntity.ok()
          .cacheControl(CacheControl.noCache())
          .contentType(MediaType.APPLICATION_JSON)
          .headers(httpHeaders -> httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON)))
          .body("OK:Imperative:%s".formatted(Thread.currentThread().toString()));

        log.info("Smoke imperative endpoint - status: {} - thread: {}", response.getStatusCode(), Thread.currentThread()); // NOPMD
        future.complete(response);
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
    }, 300, TimeUnit.MILLISECONDS);

    return future;
  }
}
