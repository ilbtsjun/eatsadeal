package com.backend.common.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "app.query-trace.enabled", havingValue = "true")
public class QueryTraceAspect {

    @Around("@annotation(queryTrace)")
    public Object trace(ProceedingJoinPoint joinPoint, QueryTrace queryTrace) throws Throwable {
        if (!QueryCounter.begin()) {
            return joinPoint.proceed();
        }
        String label = queryTrace.value().isBlank()
                ? joinPoint.getSignature().toShortString()
                : queryTrace.value();
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            Map<String, Integer> executed = QueryCounter.end();
            log.info("[QUERY TRACE] {} | queries={} | distinct={} | {}ms",
                    label, QueryCounter.total(executed), executed.size(), elapsedMs);
            executed.entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> log.info("[QUERY TRACE]   x{} {}", e.getValue(), abbreviate(e.getKey())));
        }
    }

    private String abbreviate(String sql) {
        String oneLine = sql.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 200 ? oneLine : oneLine.substring(0, 200) + "...";
    }
}