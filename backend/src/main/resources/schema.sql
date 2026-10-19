CREATE TABLE IF NOT EXISTS project (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    tech_stack  TEXT,
    demo_url    VARCHAR(500),
    github_url  VARCHAR(500),
    thumbnail_url VARCHAR(500),
    screenshot_urls TEXT,
    sort_order  INT DEFAULT 0,
    visible     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 기존 DB 마이그레이션 (컬럼이 없을 때만 추가)
ALTER TABLE project ADD COLUMN IF NOT EXISTS visible BOOLEAN DEFAULT TRUE;
-- 2026-10-19 상세 갤러리(최대 3장). 운영은 init.mode=never 라 이 파일이 돌지 않는다 — 배포 전 수동 실행 필수.
ALTER TABLE project ADD COLUMN IF NOT EXISTS screenshot_urls TEXT;

CREATE TABLE IF NOT EXISTS project_achievement (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    metric_value VARCHAR(255),
    sort_order   INT DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id)
);
