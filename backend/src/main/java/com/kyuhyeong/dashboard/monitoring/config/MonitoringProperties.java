package com.kyuhyeong.dashboard.monitoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    private List<ServiceConfig> services = new ArrayList<>();
    /**
     * 판정 주기(초). yml 키는 camelCase 그대로 — relaxed binding 이 적용되는
     * {@code @ConfigurationProperties} 경유로만 읽는다. {@code ${...}} 로 조회하지 말 것.
     * 기본값은 60 — 10초였을 때 공개 도메인 폴링이 47일간 24.4GB 아웃바운드를 만들었다.
     */
    private int checkIntervalSeconds = 60;

    @Data
    public static class ServiceConfig {
        private String name;
        private String projectSlug;
        private String healthUrl;
        private String containerName;
    }
}
