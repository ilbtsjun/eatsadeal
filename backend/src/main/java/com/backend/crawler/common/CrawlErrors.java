package com.backend.crawler.common;

public class CrawlErrors {
    private int count;
    private String first;

    public void add(String where, Exception e) {
        add(where + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    public void add(String message) {
        count++;
        if (first == null) {
            first = message;
        }
    }

    public int count() { return count; }
    public String first() { return first; }
}