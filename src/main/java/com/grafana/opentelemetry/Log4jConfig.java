package com.grafana.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@ConditionalOnClass(name = "org.apache.logging.log4j.core.LoggerContext")
public class Log4jConfig implements LogAppenderConfigurer {

  private static final Logger logger = LogManager.getLogger(Log4jConfig.class);

  public void tryAddAppender(OpenTelemetry openTelemetry) {
    org.apache.logging.log4j.spi.LoggerContext loggerContextSpi = LogManager.getContext(false);
    if (!(loggerContextSpi instanceof LoggerContext)) {
      logger.warn("cannot add log4j OpenTelemetryAppender, not running in a LoggerContext");
      return;
    }

    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    Configuration config = context.getConfiguration();
    boolean found =
        config.getAppenders().values().stream()
            .anyMatch(
                a ->
                    a
                        instanceof
                        io.opentelemetry.instrumentation.log4j.appender.v2_17
                            .OpenTelemetryAppender);
    if (found) {
      logger.info("log4j2 OpenTelemetryAppender has already been added");
      OpenTelemetryAppender.install(openTelemetry);
      return;
    }

    logger.info("adding log4j OpenTelemetryAppender");
    OpenTelemetryAppender appender =
        OpenTelemetryAppender.builder()
            .setCaptureExperimentalAttributes(true)
            .setName("OpenTelemetryAppender")
            .setConfiguration(config)
            .setOpenTelemetry(openTelemetry)
            .build();
    appender.start();
    config.addAppender(appender);

    updateLoggers(appender, config);
  }

  private static void updateLoggers(Appender appender, Configuration config) {
    for (LoggerConfig loggerConfig : config.getLoggers().values()) {
      loggerConfig.addAppender(appender, null, null);
    }
    config.getRootLogger().addAppender(appender, null, null);
  }
}
