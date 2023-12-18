package com.grafana.opentelemetry;

import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  private static final Logger LOG = LoggerFactory.getLogger(HelloController.class);

  private final Random random = new Random();

  @GetMapping("/hello")
  public String sayHello() {
    LOG.info("hello LGTM");
    if (random.nextBoolean()) {
      throw new RuntimeException("Failed to get cart");
    }
    return "hello LGTM";
  }
}
