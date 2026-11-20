package com.example.healthtwin.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PostgresDataSourceConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "DATABASE_URL")
    public DataSource dataSource(org.springframework.core.env.Environment environment) {
        String databaseUrl = environment.getRequiredProperty("DATABASE_URL").trim();
        URI databaseUri = URI.create(databaseUrl);
        String host = databaseUri.getHost();
        int port = databaseUri.getPort();
        String database = databaseUri.getRawPath();

        String jdbcUrl = "jdbc:postgresql://" + host
                + (port > 0 ? ":" + port : "")
                + database;
        if (databaseUri.getRawQuery() != null) {
            jdbcUrl += "?" + databaseUri.getRawQuery();
        } else {
            jdbcUrl += "?sslmode=require";
        }

        String userInfo = databaseUri.getRawUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalArgumentException("DATABASE_URL must include a username and password");
        }

        String[] credentials = userInfo.split(":", 2);
        String username = decode(credentials[0]);
        String password = decode(credentials[1]);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
