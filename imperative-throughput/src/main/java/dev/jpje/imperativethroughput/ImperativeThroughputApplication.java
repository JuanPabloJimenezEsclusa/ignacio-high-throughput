package dev.jpje.imperativethroughput;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class ImperativeThroughputApplication {

  private ImperativeThroughputApplication() {
  }

  public static void main(String[] args) {
    SpringApplication.run(ImperativeThroughputApplication.class, args);
  }
}
