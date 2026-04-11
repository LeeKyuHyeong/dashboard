package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.config.MonitoringProperties;
import com.kyuhyeong.dashboard.monitoring.model.ServiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final MonitoringProperties monitoringProperties;
    private final RestTemplate restTemplate;
    private final MonitoringDataHolder dataHolder;

    public void checkAll() {
        for (MonitoringProperties.ServiceConfig service : monitoringProperties.getServices()) {
            ServiceStatus status = checkService(service);
            dataHolder.updateServiceStatus(service.getContainerName(), status);
        }
    }

    private ServiceStatus checkService(MonitoringProperties.ServiceConfig service) {
        String healthStatus = "DOWN";
        long responseTime = 0;
        String dockerStatus = "unknown";
        long uptimeSeconds = 0;

        // Health check via HTTP — any HTTP response (including 4xx/5xx) means the process is alive.
        // Only connection failure / timeout is treated as DOWN.
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(service.getHealthUrl(), String.class);
            responseTime = System.currentTimeMillis() - start;
            healthStatus = "UP";
        } catch (HttpStatusCodeException e) {
            responseTime = System.currentTimeMillis() - start;
            healthStatus = "UP";
        } catch (Exception e) {
            responseTime = System.currentTimeMillis() - start;
            log.debug("Health check failed for {}: {}", service.getName(), e.getMessage());
        }

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
