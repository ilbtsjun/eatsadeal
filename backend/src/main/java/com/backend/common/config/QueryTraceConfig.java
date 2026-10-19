package com.backend.common.config;

import com.backend.common.log.QueryCounter;
import org.hibernate.cfg.JdbcSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.query-trace.enabled", havingValue = "true")
public class QueryTraceConfig {

    @Bean
    public HibernatePropertiesCustomizer queryCounterCustomizer() {
        return properties -> properties.put(JdbcSettings.STATEMENT_INSPECTOR, new QueryCounter());
    }
}