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
        String thumbnailUrl,
        List<String> screenshotUrls
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
                p.getThumbnailUrl(),
                parseStringList(p.getScreenshotUrls())
        );
    }

    static List<String> parseTechStack(String json) {
        return parseStringList(json);
    }

    /** JSON 문자열 배열 → List. null·빈 문자열·파싱 실패는 모두 빈 목록(화면이 섹션을 숨긴다). */
    static List<String> parseStringList(String json) {
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
