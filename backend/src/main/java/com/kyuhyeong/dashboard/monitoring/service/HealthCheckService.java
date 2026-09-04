package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.config.MonitoringProperties;
import com.kyuhyeong.dashboard.monitoring.model.ServiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.time.Instant;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final MonitoringProperties monitoringProperties;
    private final RestTemplate restTemplate;
    private final MonitoringDataHolder dataHolder;

    public void checkAll() {
        List<String> names = monitoringProperties.getServices().stream()
                .map(MonitoringProperties.ServiceConfig::getContainerName)
                .toList();
        Map<String, String[]> snap = snapshotContainers(names);
        for (MonitoringProperties.ServiceConfig s : monitoringProperties.getServices()) {
            dataHolder.updateServiceStatus(s.getContainerName(), checkService(s, snap));
        }
    }

    private Map<String, String[]> snapshotContainers(List<String> names) {
        Map<String, String[]> result = new HashMap<>();
        List<String> cmd = new ArrayList<>(List.of(
                "docker", "inspect", "--format",
                "{{.Name}}\t{{.State.Status}}\t{{.State.StartedAt}}"));
        cmd.addAll(names);
        Process p = null;
        try {
            p = new ProcessBuilder(cmd).start();          // redirectErrorStream 쓰지 않는다
            byte[] out = p.getInputStream().readAllBytes();   // waitFor보다 먼저
            if (!p.waitFor(3, TimeUnit.SECONDS)) { p.destroyForcibly(); return result; }
            for (String line : new String(out).split("\n")) {
                String[] f = line.trim().split("\t");
                if (f.length == 3) result.put(f[0].replaceFirst("^/", ""), f);
            }
        } catch (Exception e) {
            log.warn("docker inspect 실패: {}", e.getMessage());
            if (p != null) p.destroyForcibly();
        }
        return result;
    }

    private ServiceStatus checkService(MonitoringProperties.ServiceConfig service,
                                       Map<String, String[]> snap) {
        String healthStatus;
        long responseTime = 0;
        String dockerStatus = "unknown";
        long uptimeSeconds = 0;

        String url = service.getHealthUrl();
        if (url == null || url.isBlank()) {
            healthStatus = "UNKNOWN";              // 체크 수단 없음. UP으로 위장하지 않는다
        } else {
            long start = System.currentTimeMillis();
            try {
                restTemplate.getForEntity(url, String.class);
                healthStatus = "UP";
            } catch (HttpStatusCodeException e) {
                healthStatus = e.getStatusCode().is5xxServerError() ? "DEGRADED" : "UP";
            } catch (Exception e) {
                healthStatus = "DOWN";
                log.warn("Health check 실패 {}: {}", service.getName(), e.getMessage());
            }
            responseTime = System.currentTimeMillis() - start;
        }

        String[] f = snap.get(service.getContainerName());
        if (f != null) {
            dockerStatus = f[1];
            try {
                uptimeSeconds = Duration.between(Instant.parse(f[2]), Instant.now()).getSeconds();
            } catch (Exception ignored) { }
        }

        return ServiceStatus.builder()
                .name(service.getName())
                .projectSlug(service.getProjectSlug())
                .containerName(service.getContainerName())
                .status(healthStatus)
                .responseTimeMs(responseTime)
                .dockerStatus(dockerStatus)
                .uptimeSeconds(uptimeSeconds)
                .checkedAt(LocalDateTime.now())
                .build();
    }

    private String getDockerContainerStatus(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "inspect", "--format", "{{.State.Status}}", containerName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            if (exitCode == 0 && !output.isEmpty()) {
                return output;
            }
        } catch (Exception e) {
            log.debug("Could not get Docker status for {}: {}", containerName, e.getMessage());
        }
        return "unknown";
    }

    private long getDockerContainerUptime(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "inspect", "--format", "{{.State.StartedAt}}", containerName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            if (exitCode == 0 && !output.isEmpty()) {
                java.time.Instant startedAt = java.time.Instant.parse(output);
                return java.time.Duration.between(startedAt, java.time.Instant.now()).getSeconds();
            }
        } catch (Exception e) {
            log.debug("Could not get Docker uptime for {}: {}", containerName, e.getMessage());
        }
        return 0;
    }
}
