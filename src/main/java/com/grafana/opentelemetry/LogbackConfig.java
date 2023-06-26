package com.grafana.opentelemetry;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class LogbackConfig {

    private static final Logger logger = LoggerFactory.getLogger(LogbackConfig.class);

    static void addLogbackAppender() {
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getILoggerFactory().getLogger(Logger.ROOT_LOGGER_NAME);

        //check if appender has been added manually already
        AtomicBoolean found = new AtomicBoolean(false);
        logbackLogger.iteratorForAppenders().forEachRemaining(appender -> {
            if (appender instanceof OpenTelemetryAppender) {
                found.set(true);
            }
        });
        if (found.get()) {
            logger.info("logback OpenTelemetryAppender has already been added");
            return;
        }

        logger.info("adding logback OpenTelemetryAppender");
        OpenTelemetryAppender openTelemetryAppender = new OpenTelemetryAppender();
        openTelemetryAppender.setCaptureExperimentalAttributes(true);
        openTelemetryAppender.start();
        logbackLogger.addAppender(openTelemetryAppender);
    }

}
