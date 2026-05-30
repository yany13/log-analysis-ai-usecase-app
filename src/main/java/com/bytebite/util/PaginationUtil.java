package com.bytebite.util;

import java.util.*;

/**
 * Pagination Utility
 * Handles pagination logic for large result sets
 */
public class PaginationUtil {
    private int pageNumber;
    private int pageSize;
    private int totalItems;
    private List<?> items;

    public PaginationUtil(int pageNumber, int pageSize, int totalItems, List<?> items) {
        this.pageNumber = Math.max(1, pageNumber);
        this.pageSize = Math.min(pageSize, Constants.MAX_PAGE_SIZE);
        this.totalItems = totalItems;
        this.items = items;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    public int getStartIndex() {
        return (pageNumber - 1) * pageSize;
    }

    public int getEndIndex() {
        return Math.min(getStartIndex() + pageSize, totalItems);
    }

    public boolean hasNextPage() {
        return pageNumber < getTotalPages();
    }

    public boolean hasPreviousPage() {
        return pageNumber > 1;
    }

    public List<?> getItems() {
        return items;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("pageNumber", pageNumber);
        map.put("pageSize", pageSize);
        map.put("totalItems", totalItems);
        map.put("totalPages", getTotalPages());
        map.put("hasNextPage", hasNextPage());
        map.put("hasPreviousPage", hasPreviousPage());
        map.put("items", items);
        return map;
    }
}
