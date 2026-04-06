package com.kyuhyeong.dashboard.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyuhyeong.dashboard.domain.Project;

import java.util.List;

public record ProjectListDto(
        Long id,
        String name,
        String slug,
        String description,
        List<String> techStack,
        String demoUrl,
        String githubUrl,
        String thumbnailUrl
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ProjectListDto from(Project p) {
        return new ProjectListDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                parseTechStack(p.getTechStack()),
                p.getDemoUrl(),
                p.getGithubUrl(),
                p.getThumbnailUrl()
        );
    }

    static List<String> parseTechStack(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
