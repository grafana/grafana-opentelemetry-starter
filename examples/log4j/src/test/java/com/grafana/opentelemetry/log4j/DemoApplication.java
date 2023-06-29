package com.grafana.opentelemetry.log4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;

@SpringBootApplication
@ImportAutoConfiguration(exclude = { AutoConfigureJdbc.class }) //no idea why this is needed
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
