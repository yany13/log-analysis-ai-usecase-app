package com.bytebite.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * DateTimeUtil Tests
 */
public class DateTimeUtilTest {

    @Test
    public void testCurrentTimestamp() {
        String timestamp = DateTimeUtil.getCurrentTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.length() > 10);
    }

    @Test
    public void testTimeAgo() {
        long now = System.currentTimeMillis();
        java.util.Date past30seconds = new java.util.Date(now - 30 * 1000);
        java.util.Date past2minutes = new java.util.Date(now - 2 * 60 * 1000);
        java.util.Date past1hour = new java.util.Date(now - 60 * 60 * 1000);
        
        String ago30s = DateTimeUtil.getTimeAgo(past30seconds);
        String ago2m = DateTimeUtil.getTimeAgo(past2minutes);
        String ago1h = DateTimeUtil.getTimeAgo(past1hour);
        
        assertTrue(ago30s.contains("s ago"));
        assertTrue(ago2m.contains("m ago"));
        assertTrue(ago1h.contains("h ago"));
    }

    @Test
    public void testWithinLastHour() {
        long now = System.currentTimeMillis();
        java.util.Date within = new java.util.Date(now - 30 * 60 * 1000);
        java.util.Date outside = new java.util.Date(now - 90 * 60 * 1000);
        
        assertTrue(DateTimeUtil.isWithinLastHour(within));
        assertFalse(DateTimeUtil.isWithinLastHour(outside));
    }

    @Test
    public void testWithinLastDay() {
        long now = System.currentTimeMillis();
        java.util.Date within = new java.util.Date(now - 12 * 60 * 60 * 1000);
        java.util.Date outside = new java.util.Date(now - 48 * 60 * 60 * 1000);
        
        assertTrue(DateTimeUtil.isWithinLastDay(within));
        assertFalse(DateTimeUtil.isWithinLastDay(outside));
    }

    @Test
    public void testDurationString() {
        long start = System.currentTimeMillis();
        long end500ms = start + 500;
        long end5s = start + 5000;
        long end2m = start + 120000;
        
        String duration500ms = DateTimeUtil.getDurationString(start, end500ms);
        String duration5s = DateTimeUtil.getDurationString(start, end5s);
        String duration2m = DateTimeUtil.getDurationString(start, end2m);
        
        assertTrue(duration500ms.contains("ms"));
        assertTrue(duration5s.contains("s"));
        assertTrue(duration2m.contains("m"));
    }
}
