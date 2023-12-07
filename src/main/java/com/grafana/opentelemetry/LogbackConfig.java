package com.grafana.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@ConditionalOnClass(name = "ch.qos.logback.classic.Logger")
public class LogbackConfig implements LogAppenderConfigurer {

  private static final Logger logger = LoggerFactory.getLogger(LogbackConfig.class);

  public void tryAddAppender(OpenTelemetry openTelemetry) {
    ch.qos.logback.classic.Logger logbackLogger = getLogger();

    // check if appender has been added manually already
    if (hasAppender(logbackLogger)) {
      logger.info("logback OpenTelemetryAppender has already been added");
      OpenTelemetryAppender.install(openTelemetry);
      return;
    }

    logger.info("adding logback OpenTelemetryAppender");
    OpenTelemetryAppender appender = new OpenTelemetryAppender();
    appender.setCaptureExperimentalAttributes(true);
    appender.setOpenTelemetry(openTelemetry);
    appender.start();
    logbackLogger.addAppender(appender);
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
