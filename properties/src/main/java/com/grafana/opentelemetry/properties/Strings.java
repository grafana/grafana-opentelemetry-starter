package com.grafana.opentelemetry.properties;

//copied from org.apache.logging.log4j.util.Strings so we don't need to add a dependency
public class Strings {
    public static boolean isNotBlank(final String s) {
        return !isBlank(s);
    }

    public static boolean isBlank(final String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }
}
