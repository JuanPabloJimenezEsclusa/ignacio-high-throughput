package edu.ignacio.poc.reactivethroughput.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.Map;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The type Aggregate controller.
 *
 * <p>Demonstrates parallel fan-out using {@code Flux.merge()} to issue three
 * concurrent downstream calls without blocking any thread. Results are
 * collected into a list and combined reactively. Backpressure is naturally
 * handled by the reactive pipeline — if the downstream slows down, the
 * subscriber is not flooded. Compare with the imperative counterpart using
 * {@code CompletableFuture.allOf()}.
 */
@Configuration
public class AggregateController extends AbstractReactiveController {

  private static final Logger log = LoggerFactory.getLogger(AggregateController.class);
  private static final String API_DATA_ID = "/api/data/{id}";
  private static final int FAN_OUT = 3;

  AggregateController(
    @Value("${downstream.service.url}") @NonNull final String downstreamUrl,
    @NonNull final MeterRegistry meterRegistry
  ) {
    super(downstreamUrl, meterRegistry, "aggregate");
  }

  /**
   * Aggregate routes router function.
   *
   * @return the router function
   */
  @Bean
  public RouterFunction<ServerResponse> aggregateRoutes() {
    return route(
      GET("/aggregate").or(GET("/aggregate/")),
      _ -> {
        final var sample = Timer.start();
        final var calls = IntStream.rangeClosed(1, FAN_OUT)
          .mapToObj(i -> this.fetchData(i).map(body -> "item%d:%s".formatted(i, body.get("value"))))
          .toList();

        return Flux.merge(calls)
          .collectList()
          .flatMap(results -> {
            final var combined = String.join(",", results);
            final var currentThread = Thread.currentThread();
            log.info("Aggregate reactive endpoint - results: {} - thread: {}", combined, currentThread);
            sample.stop(this.timer);
            return this.okResponse()
              .bodyValue("OK:Reactive:Aggregate:[%s]:%s".formatted(combined, currentThread));
          })
          .doOnError(throwable -> log.error("Error in Aggregate reactive endpoint", throwable));
      });
  }

  @SuppressWarnings("unchecked")
  private Mono<Map<String, Object>> fetchData(final int index) {
    return this.webClient.get()
      .uri(API_DATA_ID, index)
      .retrieve()
      .bodyToMono(Map.class)
      .map(m -> (Map<String, Object>) m);
  }
}
