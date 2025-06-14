package edu.ignacio.poc.reactivethroughput.config;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Error handler configuration.
 */
@Configuration
public class ErrorHandlerConfiguration {

  /**
   * Error attributes.
   *
   * @return the error attributes
   */
  @Bean
  public ErrorAttributes errorAttributes() {
    return new DefaultErrorAttributes();
  }

  /**
   * Resources web properties.
   *
   * @return the web properties resources
   */
  @Bean
  public WebProperties.Resources resources() {
    return new WebProperties.Resources();
  }
}
