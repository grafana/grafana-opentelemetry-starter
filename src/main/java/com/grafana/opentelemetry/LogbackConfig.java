package com.grafana.opentelemetry;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@ConditionalOnClass(name = "ch.qos.logback.classic.Logger")
public class LogbackConfig implements LogAppenderConfigurer {

  private static final Logger logger = LoggerFactory.getLogger(LogbackConfig.class);

  public void tryAddAppender() {
    ch.qos.logback.classic.Logger logbackLogger = getLogger();

    // check if appender has been added manually already
    if (hasAppender(logbackLogger)) {
      logger.info("logback OpenTelemetryAppender has already been added");
      return;
    }

    logger.info("adding logback OpenTelemetryAppender");
    OpenTelemetryAppender openTelemetryAppender = new OpenTelemetryAppender();
    openTelemetryAppender.setCaptureExperimentalAttributes(true);
    openTelemetryAppender.start();
    logbackLogger.addAppender(openTelemetryAppender);
  }

  static ch.qos.logback.classic.Logger getLogger() {
    return (ch.qos.logback.classic.Logger)
        LoggerFactory.getILoggerFactory().getLogger(Logger.ROOT_LOGGER_NAME);
  }

  static boolean hasAppender(ch.qos.logback.classic.Logger logbackLogger) {
    AtomicBoolean found = new AtomicBoolean(false);
    logbackLogger
        .iteratorForAppenders()
        .forEachRemaining(
            appender -> {
              if (appender instanceof OpenTelemetryAppender) {
                found.set(true);
              }
            });
    return found.get();
  }
}
