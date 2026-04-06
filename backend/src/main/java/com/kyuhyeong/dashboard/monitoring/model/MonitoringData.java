package com.kyuhyeong.dashboard.monitoring.model;

import java.time.LocalDateTime;
import java.util.List;

public record MonitoringData(
        List<ServiceStatus> services,
        ServerMetric server,
        LocalDateTime timestamp
) {
}
