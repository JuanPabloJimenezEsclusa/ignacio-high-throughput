package edu.ignacio.poc.reactivethroughput.controller;

import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * The type Global exception handler.
 */
@Component
@Order(-2)
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

  /**
   * Instantiates a new Global exception handler.
   *
   * @param errorAttributes       the error attributes
   * @param applicationContext    the application context
   * @param serverCodecConfigurer the server codec configurer
   */
  public GlobalExceptionHandler(final ErrorAttributes errorAttributes,
                                final ApplicationContext applicationContext,
                                final ServerCodecConfigurer serverCodecConfigurer) {
    super(errorAttributes, new WebProperties.Resources(), applicationContext);
    super.setMessageWriters(serverCodecConfigurer.getWriters());
    super.setMessageReaders(serverCodecConfigurer.getReaders());
  }

  @NonNull
  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(final @NonNull ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), this::formatErrorResponse);
  }

  private Mono<ServerResponse> formatErrorResponse(final ServerRequest request) {
    final var errorAttributes = getErrorAttributes(request, ErrorAttributeOptions.defaults());

    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(Map.of(
        "status", Objects.requireNonNull(errorAttributes.get("status")),
        "error", Objects.requireNonNull(errorAttributes.get("error")),
        "path", Objects.requireNonNull(errorAttributes.get("path"))
      )));
  }
}
