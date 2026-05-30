package com.bytebite.util;

import java.util.*;

/**
 * Filter Utility
 * Handles filtering and searching functionality
 */
public class FilterUtil {
    private Map<String, Object> filters;
    private List<String> searchFields;
    private String searchTerm;

    public FilterUtil() {
        this.filters = new HashMap<>();
        this.searchFields = new ArrayList<>();
    }

    public void addFilter(String field, Object value) {
        if (value != null) {
            filters.put(field, value);
        }
    }

    public void setSearchTerm(String term) {
        this.searchTerm = term;
    }

    public void addSearchField(String field) {
        searchFields.add(field);
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public List<String> getSearchFields() {
        return searchFields;
    }

    public boolean hasFilters() {
        return !filters.isEmpty() || (searchTerm != null && !searchTerm.trim().isEmpty());
    }

    public String buildWhereClause() {
        StringBuilder where = new StringBuilder();
        
        // Add exact filters
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(entry.getKey()).append(" = ?");
        }

        // Add search term
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            if (where.length() > 0) {
                where.append(" AND (");
            } else {
                where.append("(");
            }
            
            for (int i = 0; i < searchFields.size(); i++) {
                if (i > 0) {
                    where.append(" OR ");
                }
                where.append(searchFields.get(i)).append(" LIKE ?");
            }
            where.append(")");
        }

        return where.length() > 0 ? "WHERE " + where.toString() : "";
    }

    public List<Object> getFilterValues() {
        List<Object> values = new ArrayList<>(filters.values());
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String pattern = "%" + searchTerm + "%";
            for (int i = 0; i < searchFields.size(); i++) {
                values.add(pattern);
            }
        }

        return values;
    }
}
