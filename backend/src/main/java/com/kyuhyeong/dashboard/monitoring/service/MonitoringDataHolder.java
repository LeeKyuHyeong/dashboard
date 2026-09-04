package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.model.MonitoringData;
import com.kyuhyeong.dashboard.monitoring.model.ServerMetric;
import com.kyuhyeong.dashboard.monitoring.model.ServiceStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitoringDataHolder {

    private final ConcurrentHashMap<String, ServiceStatus> serviceStatuses = new ConcurrentHashMap<>();
    private volatile ServerMetric serverMetric;
    private volatile Instant lastCheckedAt = Instant.now();   // 기동 시각으로 초기화

    public void updateServiceStatus(String containerName, ServiceStatus status) {
        serviceStatuses.put(containerName, status);
    }

    public void updateServerMetric(ServerMetric metric) {
        this.serverMetric = metric;
    }

    public MonitoringData getAll() {
        List<ServiceStatus> services = new ArrayList<>(serviceStatuses.values());
        return new MonitoringData(services, serverMetric, LocalDateTime.now());
    }

    public void markChecked() { this.lastCheckedAt = Instant.now(); }

    public long getLastCheckedAgeSeconds() {
        return Duration.between(lastCheckedAt, Instant.now()).getSeconds();
    }
}
