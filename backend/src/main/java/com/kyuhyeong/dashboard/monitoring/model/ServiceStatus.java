package com.kyuhyeong.dashboard.monitoring.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceStatus {
    private String name;
    private String projectSlug;
    private String containerName;
    private String status;          // "UP" or "DOWN"
    private Long responseTimeMs;
    private String dockerStatus;    // "running", "stopped", etc.
    private Long uptimeSeconds;
    private LocalDateTime checkedAt;
}
