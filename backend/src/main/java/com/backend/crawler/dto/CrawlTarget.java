package com.backend.crawler.dto;

import java.util.Arrays;
import java.util.List;

public enum CrawlTarget {
    BHC("bhc", "chicken"),
    BBQ("bbq", "chicken"),
    KYOCHON("kyochon", "chicken"),
    PELICANA("pelicana", "chicken"),
    GOOBNE("goobne", "chicken"),

    DOMINOS("dominos", "pizza"),
    PAPAJOHNS("papajohns", "pizza"),
    PIZZAMARU("pizzamaru", "pizza"),
    PIZZAETANG("pizzaetang", "pizza"),
    PIZZASCHOOL("pizzaschool", "pizza"),

    BURGERKING("burgerking", "hamburger"),
    LOTTERIA("lotteria", "hamburger"),
    KFC("kfc", "hamburger"),
    MOMSTOUCH("momstouch", "hamburger"),
    FRANKBURGER("frankburger", "hamburger");

    private final String key;
    private final String group;

    CrawlTarget(String key, String group) {
        this.key = key;
        this.group = group;
    }

    public String getKey() { return key; }
    public String getGroup() { return group; }

    public static List<CrawlTarget> byGroup(String group) {
        return Arrays.stream(values()).filter(t -> t.group.equals(group)).toList();
    }

    public static boolean isGroup(String value) {
        return Arrays.stream(values()).anyMatch(t -> t.group.equals(value));
    }

    public static CrawlTarget fromKey(String key) {
        return Arrays.stream(values())
                .filter(t -> t.key.equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }
}
