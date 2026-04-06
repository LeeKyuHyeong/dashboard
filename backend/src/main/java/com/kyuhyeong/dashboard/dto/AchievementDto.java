package com.kyuhyeong.dashboard.dto;

import com.kyuhyeong.dashboard.domain.ProjectAchievement;

public record AchievementDto(
        Long id,
        String title,
        String description,
        String metricValue,
        Integer sortOrder
) {
    public static AchievementDto from(ProjectAchievement a) {
        return new AchievementDto(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getMetricValue(),
                a.getSortOrder()
        );
    }
}
