package com.kyuhyeong.dashboard.dto;

import com.kyuhyeong.dashboard.domain.Project;

import java.util.List;

public record ProjectDetailDto(
        Long id,
        String name,
        String slug,
        String description,
        List<String> techStack,
        String demoUrl,
        String githubUrl,
        String thumbnailUrl,
        List<AchievementDto> achievements
) {
    public static ProjectDetailDto from(Project p) {
        List<AchievementDto> achievements = p.getAchievements() == null
                ? List.of()
                : p.getAchievements().stream().map(AchievementDto::from).toList();

        return new ProjectDetailDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                ProjectListDto.parseTechStack(p.getTechStack()),
                p.getDemoUrl(),
                p.getGithubUrl(),
                p.getThumbnailUrl(),
                achievements
        );
    }
}
