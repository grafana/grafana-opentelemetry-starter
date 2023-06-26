package com.grafana.opentelemetry;

import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.Logger;


public class Log4jConfig {

    private static final Logger logger = LogManager.getLogger(Log4jConfig.class);

    static boolean tryAddAppender() {
        try {
            Class.forName("org.apache.logging.log4j.core.LoggerContext");
        } catch (ClassNotFoundException e) {
            return false;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        boolean found = config.getAppenders().values().stream()
                .anyMatch(a -> a instanceof io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender);
        if (found) {
            logger.info("log4j2 OpenTelemetryAppender has already been added");
            return true;
        }

        logger.info("adding log4j OpenTelemetryAppender");
        OpenTelemetryAppender appender = OpenTelemetryAppender.builder()
                .setCaptureExperimentalAttributes(true)
                .setName("OpenTelemetryAppender")
                .setConfiguration(config)
                .build();
        appender.start();
        config.addAppender(appender);

        updateLoggers(appender, config);
        return true;
    }

    private static void updateLoggers(Appender appender, Configuration config) {
        for (LoggerConfig loggerConfig : config.getLoggers().values()) {
            loggerConfig.addAppender(appender, null, null);
        }
        config.getRootLogger().addAppender(appender, null, null);
    }
}
