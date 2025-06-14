package dev.jpje.reactivethroughput;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class ReactiveThroughputApplication {

  private ReactiveThroughputApplication() {
  }

  public static void main(String[] args) {
    SpringApplication.run(ReactiveThroughputApplication.class, args);
  }
}
