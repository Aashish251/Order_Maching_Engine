// src/main/java/com/project/ome/shared/config/DataSourceConfig.java
package com.project.ome.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    // Routes queries to primary or replica based on @Transactional(readOnly)
    @Bean
    @Primary
    public DataSource routingDataSource(
            @Value("${spring.datasource.url}")      String writeUrl,
            @Value("${spring.datasource.username}") String writeUser,
            @Value("${spring.datasource.password}") String writePass,
            @Value("${app.datasource.read-replica.url}") String readUrl,
            @Value("${app.datasource.read-replica.username}") String readUser,
            @Value("${app.datasource.read-replica.password}") String readPass) {

        DataSource writeDs = DataSourceBuilder.create()
                .url(writeUrl).username(writeUser).password(writePass).build();

        DataSource readDs = DataSourceBuilder.create()
                .url(readUrl).username(readUser).password(readPass).build();

        AbstractRoutingDataSource routing = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return TransactionContext.isReadOnly() ? "read" : "write";
            }
        };

        routing.setDefaultTargetDataSource(writeDs);
        routing.setTargetDataSources(Map.of("write", writeDs, "read", readDs));
        routing.afterPropertiesSet();
        return routing;
    }
}