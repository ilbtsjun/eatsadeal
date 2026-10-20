package com.backend.crawler.dto;

import java.util.Arrays;
import java.util.List;

public enum CrawlTarget {
    BHC("bhc", "chicken", "BHC"),
    BBQ("bbq", "chicken", "BBQ"),
    KYOCHON("kyochon", "chicken", "KyoChonChicken"),
    PELICANA("pelicana", "chicken", "Pelicana"),
    GOOBNE("goobne", "chicken", "Goobne"),

    DOMINOS("dominos", "pizza", "Dominos"),
    PAPAJOHNS("papajohns", "pizza", "Papajohns"),
    PIZZAMARU("pizzamaru", "pizza", "PizzaMaru"),
    PIZZAETANG("pizzaetang", "pizza", "Pizzaetang"),
    PIZZASCHOOL("pizzaschool", "pizza", "PizzaSchool"),

    BURGERKING("burgerking", "hamburger", "Burgerking"),
    LOTTERIA("lotteria", "hamburger", "Lotteria"),
    KFC("kfc", "hamburger", "KFC"),
    MOMSTOUCH("momstouch", "hamburger", "Momstouch"),
    FRANKBURGER("frankburger", "hamburger", "Frankburger");

    private final String key;
    private final String group;
    private final String brandName;

    CrawlTarget(String key, String group, String brandName) {
        this.key = key;
        this.group = group;
        this.brandName = brandName;
    }

    public String getKey() { return key; }
    public String getGroup() { return group; }
    public String getBrandName() { return brandName; }

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
