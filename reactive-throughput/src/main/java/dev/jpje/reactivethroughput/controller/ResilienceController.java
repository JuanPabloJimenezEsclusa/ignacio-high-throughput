package dev.jpje.reactivethroughput.controller;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * The type Resilience controller.
 *
 * <p>Demonstrates resilience patterns using the Reactor operator model.
 * {@code Mono.timeout()} signals a {@link java.util.concurrent.TimeoutException}
 * if the upstream does not emit within the deadline; {@code .onErrorReturn()} catches
 * it and substitutes a static fallback — all without blocking a single thread.
 * The entire error recovery path is declared inline as a pure pipeline transformation,
 * keeping the code concise and composable. Compare with the imperative counterpart
 * that uses {@code CompletableFuture.orTimeout()} + {@code .handle()} — identical
 * intent, different execution model.
 */
@Configuration
public class ResilienceController extends AbstractReactiveController {

  private static final String DOWNSTREAM_PATH = "/api/data";
  private static final long TIMEOUT_MS = 500L;
  private static final String FALLBACK_BODY = "FALLBACK:Reactive:Resilience:timeout";
  private static final String RESILIENCE = "resilience";

  ResilienceController(
    @Value("${downstream.service.url}") @NonNull final String downstreamUrl,
    @NonNull final MeterRegistry meterRegistry
  ) {
    super(downstreamUrl, meterRegistry, RESILIENCE);
  }

  @Bean
  public RouterFunction<ServerResponse> resilienceRoutes() {
    return route(
      GET("/resilience").or(GET("/resilience/")),
      request -> {
        final var sample = Timer.start();
        final var delayMs = request.queryParam("delayMs")
          .map(Long::parseLong)
          .orElse(0L);

        return this.webClient.get()
          .uri(uriBuilder -> {
            uriBuilder.path(DOWNSTREAM_PATH);
            if (delayMs > 0) {
              uriBuilder.queryParam("delayMs", delayMs);
            }
            return uriBuilder.build();
          })
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofMillis(TIMEOUT_MS))
          .onErrorReturn(FALLBACK_BODY)
          .flatMap(body -> {
            sample.stop(this.timer);
            final var currentThread = Thread.currentThread();
            final var responseBody = FALLBACK_BODY.equals(body)
              ? FALLBACK_BODY
              : "OK:Reactive:Resilience:%s:%s".formatted(body, currentThread);
            return this.okResponse().bodyValue(responseBody);
          });
      });
  }
}
