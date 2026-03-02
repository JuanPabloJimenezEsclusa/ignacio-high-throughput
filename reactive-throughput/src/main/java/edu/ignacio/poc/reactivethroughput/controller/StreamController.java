package edu.ignacio.poc.reactivethroughput.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;

/**
 * The type Stream controller.
 *
 * <p>Demonstrates reactive backpressure via Server-Sent Events (SSE).
 * Emits a bounded stream of {@value STREAM_SIZE} events, one every
 * {@value EMIT_INTERVAL_MS} ms, using {@code Flux.generate} with demand-driven
 * production. The subscriber (HTTP client) controls the pace — if it cannot
 * consume fast enough, production is slowed automatically. This capability is
 * <b>fundamentally impossible</b> to replicate with virtual threads: a thread
 * per connection model cannot propagate backpressure from the consumer back to
 * the producer without blocking the producing thread.
 * This endpoint exists only in the reactive module.
 */
@Configuration
public class StreamController {

  private static final Logger log = LoggerFactory.getLogger(StreamController.class);
  private static final int STREAM_SIZE = 10;
  private static final long EMIT_INTERVAL_MS = 200L;

  private final Timer streamTimer;

  StreamController(@NonNull final MeterRegistry meterRegistry) {
    this.streamTimer = Timer.builder("http.request.duration")
      .description("Stream endpoint request duration")
      .tag("module", "reactive")
      .tag("endpoint", "stream")
      .register(meterRegistry);
  }

  /**
   * Stream routes router function.
   *
   * @return the router function
   */
  @Bean
  public RouterFunction<ServerResponse> streamRoutes() {
    return route(
      GET("/stream").or(GET("/stream/")),
      _ -> {
        final var sample = Timer.start();
        final var counter = new AtomicInteger(0);
        final var currentThread = Thread.currentThread();

        final var events = Flux.generate(
            sink -> {
              final var n = counter.incrementAndGet();
              sink.next("event:%d:thread:%s".formatted(n, currentThread.getName()));
              if (n >= STREAM_SIZE) {
                sink.complete();
              }
            })
          .cast(String.class)
          .delayElements(Duration.ofMillis(EMIT_INTERVAL_MS))
          .doOnNext(event -> log.debug("Stream reactive endpoint - emitting: {}", event))
          .doOnComplete(() -> {
            sample.stop(this.streamTimer);
            log.info("Stream reactive endpoint - completed {} events - thread: {}",
              STREAM_SIZE, currentThread);
          })
          .doOnError(throwable -> log.error("Error in stream reactive endpoint", throwable));

        return ok()
          .contentType(MediaType.TEXT_EVENT_STREAM)
          .body(events, String.class);
      });
  }
}
