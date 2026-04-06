package com.kyuhyeong.dashboard.monitoring.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServerMetric {
    private double cpuUsage;        // percentage 0-100
    private int cpuCores;
    private long memoryUsedBytes;
    private long memoryTotalBytes;
    private long diskUsedBytes;
    private long diskTotalBytes;
    private LocalDateTime collectedAt;
}
