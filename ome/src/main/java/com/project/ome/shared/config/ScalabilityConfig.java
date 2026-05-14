// src/main/java/com/project/ome/shared/config/ScalabilityConfig.java
package com.project.ome.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.consistency")
public class ScalabilityConfig {

    private int    balanceCacheTtlSeconds   = 30;
    private int    instrumentCacheTtlMinutes = 60;
    private int    orderbookDepth           = 10;
    private String kafkaAcks               = "all";
}