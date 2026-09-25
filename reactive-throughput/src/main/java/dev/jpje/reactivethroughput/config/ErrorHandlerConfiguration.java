package dev.jpje.reactivethroughput.config;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.error.DefaultErrorAttributes;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorHandlerConfiguration {

  @Bean
  public ErrorAttributes errorAttributes() {
    return new DefaultErrorAttributes();
  }

  @Bean
  public WebProperties.Resources resources() {
    return new WebProperties.Resources();
  }
}
