package com.bytebite.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Map;

/**
 * PaginationUtil Tests
 */
public class PaginationUtilTest {

    @Test
    public void testPaginationBasics() {
        PaginationUtil pagination = new PaginationUtil(1, 10, 100, null);
        
        assertEquals(1, pagination.getPageNumber());
        assertEquals(10, pagination.getPageSize());
        assertEquals(100, pagination.getTotalItems());
        assertEquals(10, pagination.getTotalPages());
    }

    @Test
    public void testPageBoundaries() {
        PaginationUtil pagination = new PaginationUtil(2, 10, 100, null);
        
        assertEquals(10, pagination.getStartIndex());
        assertEquals(20, pagination.getEndIndex());
        assertTrue(pagination.hasNextPage());
        assertTrue(pagination.hasPreviousPage());
    }

    @Test
    public void testLastPage() {
        PaginationUtil pagination = new PaginationUtil(10, 10, 100, null);
        
        assertEquals(90, pagination.getStartIndex());
        assertEquals(100, pagination.getEndIndex());
        assertFalse(pagination.hasNextPage());
        assertTrue(pagination.hasPreviousPage());
    }

    @Test
    public void testFirstPage() {
        PaginationUtil pagination = new PaginationUtil(1, 10, 100, null);
        
        assertEquals(0, pagination.getStartIndex());
        assertEquals(10, pagination.getEndIndex());
        assertTrue(pagination.hasNextPage());
        assertFalse(pagination.hasPreviousPage());
    }

    @Test
    public void testPageSizeLimit() {
        // Page size should be limited to MAX_PAGE_SIZE
        PaginationUtil pagination = new PaginationUtil(1, 500, 100, null);
        
        assertTrue(pagination.getPageSize() <= Constants.MAX_PAGE_SIZE);
    }

    @Test
    public void testInvalidPageNumber() {
        // Page number should be at least 1
        PaginationUtil pagination = new PaginationUtil(0, 10, 100, null);
        
        assertEquals(1, pagination.getPageNumber());
    }

    @Test
    public void testToMap() {
        PaginationUtil pagination = new PaginationUtil(1, 10, 100, null);
        Map<String, Object> map = pagination.toMap();
        
        assertNotNull(map);
        assertEquals(1, map.get("pageNumber"));
        assertEquals(10, map.get("pageSize"));
        assertEquals(100, map.get("totalItems"));
        assertEquals(10, map.get("totalPages"));
        assertEquals(true, map.get("hasNextPage"));
        assertEquals(false, map.get("hasPreviousPage"));
    }
}
