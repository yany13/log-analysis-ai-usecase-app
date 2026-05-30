package com.bytebite.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Date/Time Utility
 * Provides common date/time operations
 */
public class DateTimeUtil {
    private static final Logger logger = LoggerFactory.getLogger(DateTimeUtil.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(ISO_FORMATTER);
    }

    public static String formatDate(Date date) {
        if (date == null) return null;
        return DATE_FORMATTER.format(date);
    }

    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static String getTimeAgo(Date date) {
        if (date == null) return "Unknown";
        
        long secondsAgo = (System.currentTimeMillis() - date.getTime()) / 1000;
        
        if (secondsAgo < 60) {
            return secondsAgo + "s ago";
        } else if (secondsAgo < 3600) {
            return (secondsAgo / 60) + "m ago";
        } else if (secondsAgo < 86400) {
            return (secondsAgo / 3600) + "h ago";
        } else {
            return (secondsAgo / 86400) + "d ago";
        }
    }

    public static boolean isWithinLastHour(Date date) {
        if (date == null) return false;
        long minutesAgo = (System.currentTimeMillis() - date.getTime()) / (1000 * 60);
        return minutesAgo <= 60;
    }

    public static boolean isWithinLastDay(Date date) {
        if (date == null) return false;
        long hoursAgo = (System.currentTimeMillis() - date.getTime()) / (1000 * 60 * 60);
        return hoursAgo <= 24;
    }

    public static String getDurationString(long startMillis, long endMillis) {
        long durationMs = endMillis - startMillis;
        
        if (durationMs < 1000) {
            return durationMs + "ms";
        } else if (durationMs < 60000) {
            return (durationMs / 1000) + "s";
        } else if (durationMs < 3600000) {
            return (durationMs / 60000) + "m";
        } else {
            return (durationMs / 3600000) + "h";
        }
    }
}
