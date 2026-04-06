package com.kyuhyeong.dashboard.monitoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    private List<ServiceConfig> services = new ArrayList<>();
    private int checkIntervalSeconds = 10;

    @Data
    public static class ServiceConfig {
        private String name;
        private String projectSlug;
        private String healthUrl;
        private String containerName;
    }
}
