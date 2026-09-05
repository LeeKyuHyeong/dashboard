package com.kyuhyeong.dashboard.monitoring.controller;

import com.kyuhyeong.dashboard.monitoring.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.kyuhyeong.dashboard.monitoring.config.MonitoringProperties;
import com.kyuhyeong.dashboard.monitoring.service.DockerCli;
import com.kyuhyeong.dashboard.monitoring.service.MonitoringDataHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private static final Duration LOG_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_LOG_TAIL = 1000;

    private final SseEmitterService sseEmitterService;
    private final MonitoringProperties props;
    private final MonitoringDataHolder dataHolder;
    private final DockerCli docker;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseEmitterService.createEmitter();
    }

    /**
     * ⚠ 이 경로는 무인증 공개다. 컨테이너 로그 전문을 반환하므로
     * <b>호스트 nginx 에서 관리 IP 로 제한</b>한 상태를 전제한다(앱 레벨 인증은 2단계).
     * 서브프로세스 호출에는 유한 타임아웃이 걸린다 — 이전 구현은 Tomcat 워커 스레드에서
     * readAllBytes()/waitFor() 를 무한 대기했다.
     */
    @GetMapping("/logs/{containerName}")
    public ResponseEntity<String> getLogs(
            @PathVariable String containerName,
            @RequestParam(defaultValue = "100") int tail
    ) {
        int safeTail = Math.max(1, Math.min(tail, MAX_LOG_TAIL));
        DockerCli.Result result = docker.exec(
                List.of("docker", "logs", "--tail", String.valueOf(safeTail), containerName),
                LOG_TIMEOUT);

        if (result.ok()) {
            // 앱 로그는 stderr 로도 나온다. 성공한 호출에서만 둘을 합친다.
            return ResponseEntity.ok(result.stdout() + result.stderr());
        }
        log.warn("로그 조회 실패 {}: {}", containerName, result.diagnostic());
        // 실패를 200 으로 감싸면 화면에서 로그 본문과 구분되지 않는다.
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("로그를 가져오지 못했습니다 (" + result.diagnostic() + ")");
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
