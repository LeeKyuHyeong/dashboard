package com.kyuhyeong.dashboard.monitoring.controller;

import com.kyuhyeong.dashboard.monitoring.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.kyuhyeong.dashboard.monitoring.config.MonitoringProperties;
import com.kyuhyeong.dashboard.monitoring.service.MonitoringDataHolder;
import org.springframework.http.ResponseEntity;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final SseEmitterService sseEmitterService;
    private final MonitoringProperties props;
    private final MonitoringDataHolder dataHolder;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseEmitterService.createEmitter();
    }

    @GetMapping("/logs/{containerName}")
    public String getLogs(
            @PathVariable String containerName,
            @RequestParam(defaultValue = "100") int tail
    ) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "logs", "--tail", String.valueOf(tail), containerName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output;
            }
            return "Failed to retrieve logs (exit code: " + exitCode + "): " + output;
        } catch (Exception e) {
            log.warn("Docker not available for logs: {}", e.getMessage());
            return "Docker not available";
        }
    }
    /**
     * dashboard 자신의 판정 루프가 살아 있는지만 답한다. UptimeRobot 이 5분마다 친다.
     * <ul>
     *   <li>200 — 판정 루프가 최근에 완주했다</li>
     *   <li>503 — 판정 루프가 멈췄다 (한 번도 못 돌았거나, 임계 초과)</li>
     *   <li>500 — 판정 자체가 불가 (사이클이 예외로 끝남)</li>
     * </ul>
     * <b>감시 대상 컨테이너가 죽은 것은 여기에 반영하지 않는다.</b> 반영하면 컨테이너 하나가
     * 죽을 때마다 외부 감시가 "사이트 다운"으로 읽고, 호스트·nginx·dashboard 자체의 다운과
     * 구분이 불가능해진다. 무인증 공개 경로이므로 본문에 컨테이너 이름·상태를 담지 않는다.
     */
    @GetMapping("/health/self")
    public ResponseEntity<Map<String, Object>> self() {
        String failure = dataHolder.getLastFailureReason();
        Long age = dataHolder.getLastCheckedAgeSeconds();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("lastCheckAgeSec", age);   // 첫 사이클 완주 전이면 null

        if (failure != null) {
            body.put("ok", false);
            body.put("reason", "COLLECTION_FAILED");
            return ResponseEntity.status(500).body(body);
        }
        if (age == null) {
            body.put("ok", false);
            body.put("reason", "NOT_STARTED");
            return ResponseEntity.status(503).body(body);
        }
        if (age >= props.getCheckIntervalSeconds() * 3L) {
            body.put("ok", false);
            body.put("reason", "STALE");
            return ResponseEntity.status(503).body(body);
        }
        body.put("ok", true);
        return ResponseEntity.ok(body);
    }
}
