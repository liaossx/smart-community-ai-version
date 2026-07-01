package com.lsx.ai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class AiDataSourceConfig {

    @Bean
    public DataSource aiDataSource(
            @Value("${smart-community.ai.customer-service.jdbc.url}") String jdbcUrl,
            @Value("${smart-community.ai.customer-service.jdbc.username}") String username,
            @Value("${smart-community.ai.customer-service.jdbc.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        config.setPoolName("ai-pool");
        return new HikariDataSource(config);
    }
}
