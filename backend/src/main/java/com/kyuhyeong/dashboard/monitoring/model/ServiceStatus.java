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
    /** UP(running) / DOWN(존재하나 미기동) / MISSING(삭제됨) / UNKNOWN(판정 불가) */
    private String status;
    /** docker 가 보고한 원문 상태: running, exited, restarting, none, unknown */
    private String dockerStatus;
    private Long uptimeSeconds;
    private LocalDateTime checkedAt;
}
