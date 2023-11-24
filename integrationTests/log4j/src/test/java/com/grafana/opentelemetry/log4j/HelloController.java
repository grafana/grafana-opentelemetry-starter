package com.grafana.opentelemetry.log4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
  @GetMapping("/hello")
  public String sayHello() {
    return "hello LGTM";
  }
}
