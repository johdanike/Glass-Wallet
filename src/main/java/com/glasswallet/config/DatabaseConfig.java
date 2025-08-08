package com.glasswallet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Configuration
@Profile("prod")
@Slf4j
public class DatabaseConfig {
    
    private final DataSource dataSource;
    
    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseInfo() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            log.info("Database URL: {}", metaData.getURL());
            log.info("Database Product: {} {}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
            
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            log.info("Available tables:");
            while (tables.next()) {
                log.info("  - {}", tables.getString("TABLE_NAME"));
            }
        } catch (Exception e) {
            log.error("Failed to get database info: {}", e.getMessage());
        }
    }

}