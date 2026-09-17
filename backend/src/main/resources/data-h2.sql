-- Insert projects (H2)
MERGE INTO project (id, name, slug, description, tech_stack, demo_url, github_url, thumbnail_url, sort_order, visible) KEY(id) VALUES
-- id 1: itsm 저장소는 2026-09-16 public 전환(히스토리 정리 후), 설명·성과는 실제 앱 내용으로 교체.
-- 2026-09-17: 운영 DB 에 맞춰 visible=FALSE·github_url=NULL — 시연 계정 오류 등 정리 전까지 카드 비공개(상세도 404, bd95ec7).
(1, 'ITSM', 'itsm', '장애·서비스요청·변경·자산(CMDB)·정기점검·보고·게시판을 다루는 ITSM 웹 애플리케이션. Spring Boot 4 멀티모듈 + Vue 3 SPA, 메뉴 테이블 기반 3계층 RBAC, CI 테스트 게이트와 헬스체크 실패 시 자동 롤백 배포.',
 '["Vue.js", "Spring Boot", "MariaDB", "Docker"]',
 'https://itsm.kyuhyeong.com', NULL, '/thumbnails/itsm.png', 1, FALSE),
(2, 'Song Quiz', 'song-quiz', '실시간 멀티플레이어 노래 맞추기 게임. 짧은 음악 클립을 듣고 누가 가장 빠르게 곡을 맞추는지 겨루는 게임입니다.',
 '["Spring Boot", "WebSocket", "MariaDB", "Docker"]',
 'https://game.kyuhyeong.com', 'https://github.com/LeeKyuHyeong/quiz', '/thumbnails/song-quiz.png', 2, TRUE),
-- id 3 은 구 kh-shop 자리 (2026-07-23 서비스 종료) — account 가 승계
-- github_url: account 저장소 2026-09-12 private → 2026-09-16 히스토리 정리 후 public 재전환, 링크 복원.
-- 프론트(ProjectHeader.jsx)가 null 이면 GitHub 버튼 자체를 렌더링하지 않는다.
(3, 'Account', 'account', '부부/가구 단위 가계부 앱. 영수증 사진을 Claude Vision 으로 분석해 지출을 자동 분류·기록합니다.',
 '["Spring Boot", "MariaDB", "Docker", "Claude Vision"]',
 'https://account.kyuhyeong.com', 'https://github.com/LeeKyuHyeong/account', '/thumbnails/account.png', 3, TRUE);

-- Insert achievements
MERGE INTO project_achievement (id, project_id, title, description, metric_value, sort_order) KEY(id) VALUES
(1, 1, '메뉴 기반 3계층 RBAC', '역할 11종 × 메뉴 30개 read/write 매트릭스를 DB로 관리하고 Spring Security → 인터셉터(요청 URI ↔ 메뉴 URL) → @PreAuthorize 3계층으로 검사', '역할 11종 × 메뉴 30개', 1),
(2, 1, 'CI 테스트 게이트와 자동 롤백', '백엔드 660건·프론트 Vitest 164건 테스트를 CI에서 실행해 통과 시에만 이미지 빌드, 배포 후 헬스체크 실패 시 직전 SHA 이미지로 자동 롤백', '테스트 660 + 164건', 2),
(3, 2, '실시간 멀티플레이어', 'STOMP/SockJS WebSocket push + HTTP 입력, 연결 실패 시 polling fallback으로 방 기반 멀티플레이 제공', '방당 기본 8인', 1),
(4, 2, '동시성 3중 방어', '정답 처리·곡 선택의 동시 요청 경합을 방별 상태(ConcurrentHashMap)·방 단위 락·JPA @Version 세 층으로 차단', NULL, 2),
-- id 5, 6 은 구 kh-shop 성과 자리 — 5 를 account 가 승계
(5, 3, '영수증 자동 분류', 'Claude Vision 으로 영수증 사진을 분석해 품목·금액·카테고리를 자동 입력', NULL, 1);
