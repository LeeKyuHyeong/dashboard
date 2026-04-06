package com.kyuhyeong.dashboard.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringScheduler {

    private final HealthCheckService healthCheckService;
    private final ServerMetricService serverMetricService;
    private final SseEmitterService sseEmitterService;

    @Scheduled(fixedDelayString = "#{${monitoring.check-interval-seconds:10} * 1000}")
    public void collectAndBroadcast() {
        try {
            healthCheckService.checkAll();
            serverMetricService.collect();
            sseEmitterService.broadcast();
        } catch (Exception e) {
            log.error("Error during monitoring collection: {}", e.getMessage(), e);
        }
    }
}
