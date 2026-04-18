package edu.ignacio.poc.imperativethroughput.controller;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Cpu controller.
 *
 * <p>Demonstrates CPU-bound work offloaded to a virtual-thread executor.
 * Each request computes a Fibonacci number (n=50) on a virtual thread,
 * freeing the carrier thread immediately. Compare with the reactive
 * counterpart that uses {@code Mono.fromCallable} + {@code boundedElastic()}.
 */
@RestController
class CpuController extends AbstractImperativeController {

  private static final Logger log = LoggerFactory.getLogger(CpuController.class);
  private static final int FIBONACCI_N = 50;

  CpuController(final MeterRegistry meterRegistry) {
    super(meterRegistry, "cpu");
  }

  /**
   * Gets cpu.
   *
   * @return the cpu result
   */
  @GetMapping({"/cpu", "/cpu/"})
  public CompletableFuture<ResponseEntity<String>> getCpu() {
    final var future = new CompletableFuture<ResponseEntity<String>>();
    try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) { // NOPMD
      executor.execute(() -> {
        final var sample = Timer.start();
        try {
          final var result = this.fibonacci();
          final var currentThread = Thread.currentThread();
          final var response = this.okResponse()
            .body("OK:Imperative:CPU:fib(%d)=%d:%s".formatted(FIBONACCI_N, result, currentThread));

          log.info("CPU imperative endpoint - fib({})={} - thread: {}", FIBONACCI_N, result, currentThread);
          sample.stop(this.timer);
          future.complete(response);
        } catch (final Exception e) {
          log.error("Error in CPU imperative endpoint", e);
          future.completeExceptionally(e);
        }
      });
    }

    return future;
  }

  private BigInteger fibonacci() {
    if (FIBONACCI_N <= 1) {
      return BigInteger.valueOf(FIBONACCI_N);
    }
    var a = BigInteger.ZERO;
    var b = BigInteger.ONE;
    for (int i = 2; i <= FIBONACCI_N; i++) {
      final var temp = a.add(b);
      a = b;
      b = temp;
    }
    return b;
  }
}
