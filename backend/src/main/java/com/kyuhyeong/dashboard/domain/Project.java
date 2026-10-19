package com.kyuhyeong.dashboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "tech_stack", columnDefinition = "JSON")
    private String techStack;

    @Column(name = "demo_url")
    private String demoUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    /** 상세 갤러리 스크린샷 URL 배열(JSON, 최대 3장 — 초과분은 화면이 자른다). 비면 화면은 thumbnailUrl 1장으로 대체. */
    @Column(name = "screenshot_urls", columnDefinition = "JSON")
    private String screenshotUrls;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "visible")
    private Boolean visible;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ProjectAchievement> achievements = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
