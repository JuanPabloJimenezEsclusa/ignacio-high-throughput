package edu.ignacio.poc.imperativethroughput.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * The type Io controller.
 *
 * <p>Demonstrates I/O-bound work using a blocking {@link RestClient} call
 * on a virtual thread. The carrier OS thread is unmounted while the virtual
 * thread is parked waiting for the network response, allowing the JVM to
 * reuse the carrier for other work. Compare with the reactive counterpart
 * that uses a non-blocking {@code WebClient}.
 */
@RestController
class IoController extends AbstractImperativeController {

  private static final Logger log = LoggerFactory.getLogger(IoController.class);
  private static final String DOWNSTREAM_PATH = "/api/data";

  IoController(
    @Value("${downstream.service.url}") final String downstreamUrl,
    final MeterRegistry meterRegistry
  ) {
    super(downstreamUrl, meterRegistry, "io");
  }

  /**
   * Gets io.
   *
   * @return the io result
   */
  @GetMapping({"/io", "/io/"})
  public CompletableFuture<ResponseEntity<String>> getIo() {
    final var future = new CompletableFuture<ResponseEntity<String>>();

    try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) { // NOPMD
      executor.execute(() -> {
        final var sample = Timer.start();
        final var currentThread = Thread.currentThread();
        try {
          final var body = this.restClient.get()
            .uri(DOWNSTREAM_PATH)
            .retrieve()
            .body(String.class);

          final var response = this.okResponse()
            .body("OK:Imperative:IO:%s:%s".formatted(body, currentThread));

          log.info("IO imperative endpoint - downstream: {} - thread: {}", body, currentThread);
          sample.stop(this.timer);
          future.complete(response);
        } catch (final Exception e) {
          log.error("Error in IO imperative endpoint", e);
          future.completeExceptionally(e);
        }
      });
    }

    return future;
  }
}
