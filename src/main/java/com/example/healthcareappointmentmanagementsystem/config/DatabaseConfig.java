package com.example.healthcareappointmentmanagementsystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Intelligent Database Configuration that supports:
 * 1. Cloud-native DATABASE_URL URIs (Render, Railway, Heroku: postgres:// or postgresql://)
 * 2. Standard JDBC URLs (jdbc:postgresql://...)
 * 3. Individual environment variables (DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD) with safe local defaults.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${DATABASE_URL:}") String databaseUrl,
            @Value("${DB_HOST:localhost}") String dbHost,
            @Value("${DB_PORT:5432}") String dbPort,
            @Value("${DB_NAME:healthcare_db}") String dbName,
            @Value("${DB_USERNAME:postgres}") String dbUser,
            @Value("${DB_PASSWORD:}") String dbPass) {

        // 1. Check if cloud DATABASE_URL is provided in postgres:// or postgresql:// URI format
        if (databaseUrl != null && !databaseUrl.trim().isEmpty()
                && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            try {
                // Normalize scheme for URI parser
                String normalizedUriString = databaseUrl.trim().replaceFirst("^postgres://", "postgresql://");
                URI dbUri = URI.create(normalizedUriString);

                String username = dbUser;
                String password = dbPass;

                if (dbUri.getUserInfo() != null) {
                    String[] userParts = dbUri.getUserInfo().split(":", 2);
                    if (userParts.length > 0 && !userParts[0].isEmpty()) {
                        username = URLDecoder.decode(userParts[0], StandardCharsets.UTF_8);
                    }
                    if (userParts.length > 1) {
                        password = URLDecoder.decode(userParts[1], StandardCharsets.UTF_8);
                    }
                }

                String host = dbUri.getHost();
                int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
                String path = dbUri.getPath(); // includes leading slash, e.g. /dbname
                String query = dbUri.getQuery(); // e.g. sslmode=require if any

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isEmpty()) {
                    jdbcUrl += "?" + query;
                }

                log.info("Configured PostgreSQL DataSource from cloud DATABASE_URL targeting host: {}, database: {}", host, path);

                return DataSourceBuilder.create()
                        .driverClassName("org.postgresql.Driver")
                        .url(jdbcUrl)
                        .username(username)
                        .password(password)
                        .build();
            } catch (Exception e) {
                log.warn("Failed to parse DATABASE_URL as URI, falling back to standard configuration: {}", e.getMessage());
            }
        }

        // 2. Direct JDBC URL or individual host/port/db fallback (e.g. Local development)
        String jdbcUrl;
        if (databaseUrl != null && databaseUrl.trim().startsWith("jdbc:postgresql://")) {
            jdbcUrl = databaseUrl.trim();
        } else {
            jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
        }

        log.info("Configured PostgreSQL DataSource targeting: {}", jdbcUrl);

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(jdbcUrl)
                .username(dbUser)
                .password(dbPass)
                .build();
    }
}
