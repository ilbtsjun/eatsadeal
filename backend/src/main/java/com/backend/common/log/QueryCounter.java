package com.backend.common.log;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryCounter implements StatementInspector {

    private static final ThreadLocal<Map<String, Integer>> CURRENT = new ThreadLocal<>();

    public static boolean begin() {
        if (CURRENT.get() != null) {
            return false;
        }
        CURRENT.set(new LinkedHashMap<>());
        return true;
    }

    public static Map<String, Integer> end() {
        Map<String, Integer> executed = CURRENT.get();
        CURRENT.remove();
        return executed == null ? Map.of() : executed;
    }

    public static int total(Map<String, Integer> executed) {
        return executed.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public String inspect(String sql) {
        Map<String, Integer> executed = CURRENT.get();
        if (executed != null) {
            executed.merge(sql, 1, Integer::sum);
        }
        return sql;
    }
}