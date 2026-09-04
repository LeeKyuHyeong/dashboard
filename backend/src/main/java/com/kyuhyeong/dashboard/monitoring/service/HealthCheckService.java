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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final MonitoringProperties monitoringProperties;
    private final RestTemplate restTemplate;
    private final MonitoringDataHolder dataHolder;

    public void checkAll() {
        List<String> names = props.getServices().stream()
                .map(ServiceConfig::getContainerName).toList();
        Map<String, String[]> snap = snapshotContainers(names);
        for (ServiceConfig s : props.getServices()) {
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

    private ServiceStatus checkService(MonitoringProperties.ServiceConfig service) {
        String healthStatus = "DOWN";
        long responseTime = 0;
        String dockerStatus = "unknown";
        long uptimeSeconds = 0;

        // Health check via HTTP — any HTTP response (including 4xx/5xx) means the process is alive.
        // Only connection failure / timeout is treated as DOWN.
        long start = System.currentTimeMillis();
        if (service.getHealthUrl() == null || service.getHealthUrl().isBlank()) {
            healthStatus = "UNKNOWN";
        } else {
            try {
                restTemplate.getForEntity(service.getHealthUrl(), String.class);
                healthStatus = "UP";
            } catch (HttpStatusCodeException e) {
                healthStatus = e.getStatusCode().is5xxServerError() ? "DEGRADED" : "UP";
            } catch (Exception e) {
                healthStatus = "DOWN";
                log.warn("Health check 실패 {}: {}", service.getName(), e.getMessage());
            }
            responseTime = System.currentTimeMillis() - start;
        }

        /*try {
            ResponseEntity<String> response = restTemplate.getForEntity(service.getHealthUrl(), String.class);
            responseTime = System.currentTimeMillis() - start;
            healthStatus = "UP";
        } catch (HttpStatusCodeException e) {
            responseTime = System.currentTimeMillis() - start;
            healthStatus = "UP";
        } catch (Exception e) {
            responseTime = System.currentTimeMillis() - start;
            log.debug("Health check failed for {}: {}", service.getName(), e.getMessage());
        }*/

        // Docker status - try to get container info
        try {
            dockerStatus = getDockerContainerStatus(service.getContainerName());
            uptimeSeconds = getDockerContainerUptime(service.getContainerName());
        } catch (Exception e) {
            log.debug("Docker check failed for {}: {}", service.getContainerName(), e.getMessage());
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
