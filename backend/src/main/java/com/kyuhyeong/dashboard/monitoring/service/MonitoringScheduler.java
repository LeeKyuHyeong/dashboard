package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.config.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 판정 사이클 스케줄러.
 * <p>주기를 {@code @Scheduled(fixedDelayString = "${monitoring.check-interval-seconds:...}")} 로
 * 잡지 않는다. yml 의 키는 {@code monitoring.checkIntervalSeconds}(camelCase)인데
 * relaxed binding 은 {@code @ConfigurationProperties} 에만 적용되고 {@code ${...}} 조회에는
 * 적용되지 않아, 플레이스홀더가 키를 못 찾고 <b>기본값으로 조용히 떨어진다</b>.
 * 실제로 이 프로젝트에서 yml 에 60 을 적어둔 채 기본값 10초로 6주간 폴링한 전례가 있다.
 * 그래서 주기는 {@link MonitoringProperties} 한 곳에서만 읽는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringScheduler implements SchedulingConfigurer {

    private final HealthCheckService healthCheckService;
    private final ServerMetricService serverMetricService;
    private final SseEmitterService sseEmitterService;
    private final MonitoringDataHolder dataHolder;
    private final MonitoringProperties properties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        int seconds = properties.getCheckIntervalSeconds();
        // 기동 로그에 실제 적용된 주기를 남긴다 — 위 6주 사고는 이 한 줄이 있었으면 첫날 잡혔다.
        log.info("모니터링 판정 주기 {}초 (monitoring.checkIntervalSeconds)", seconds);
        registrar.addFixedDelayTask(this::collectAndBroadcast, Duration.ofSeconds(seconds));
    }

    public void collectAndBroadcast() {
        try {
            boolean decidable = healthCheckService.checkAll();
            serverMetricService.collect();
            // markChecked/markUndecidable 는 broadcast 앞에 — SSE가 막혀도 판정은 끝난 것
            if (decidable) {
                dataHolder.markChecked();
            } else {
                dataHolder.markUndecidable("DOCKER_UNAVAILABLE");
            }
            sseEmitterService.broadcast();
        } catch (Exception e) {
            // markChecked() 에 도달하지 못했으므로 lastCheckedAt 은 갱신되지 않는다.
            // 사유를 남겨 /health/self 가 503(정지)과 500(판정 불가)을 구분할 수 있게 한다.
            dataHolder.markFailed(e.getClass().getSimpleName());
            log.error("판정 사이클 실패: {}", e.getMessage(), e);
        }
    }
}
