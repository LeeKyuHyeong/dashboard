-- Insert projects (H2)
MERGE INTO project (id, name, slug, description, tech_stack, demo_url, github_url, thumbnail_url, sort_order) KEY(id) VALUES
(1, 'ITSM', 'itsm', 'Vue.js 프론트엔드와 Spring Boot 백엔드로 구축한 IT 서비스 관리 시스템. IT 서비스 요청, 장애, 변경 사항을 체계적으로 관리합니다.',
 '["Vue.js", "Spring Boot", "MariaDB", "Docker"]',
 'https://itsm.kyuhyeong.com', 'https://github.com/rbgud/itsm', '/thumbnails/itsm.png', 1),
(2, 'Song Quiz', 'song-quiz', '실시간 멀티플레이어 노래 맞추기 게임. 짧은 음악 클립을 듣고 누가 가장 빠르게 곡을 맞추는지 겨루는 게임입니다.',
 '["React", "Spring Boot", "WebSocket", "Redis", "Docker"]',
 'https://game.kyuhyeong.com', 'https://github.com/rbgud/song-quiz', '/thumbnails/song-quiz.png', 2),
(3, 'KH Shop', 'kh-shop', '상품 카탈로그, 장바구니, 주문 관리, 결제 연동을 갖춘 풀스택 이커머스 플랫폼.',
 '["React", "Spring Boot", "MariaDB", "Redis", "Docker"]',
 'https://shop.kyuhyeong.com', 'https://github.com/rbgud/kh-shop', '/thumbnails/kh-shop.png', 3);

-- Insert achievements
MERGE INTO project_achievement (id, project_id, title, description, metric_value, sort_order) KEY(id) VALUES
(1, 1, '서비스 요청 자동화', 'IT 서비스 요청 워크플로우를 자동화하여 수작업 처리 시간을 단축', '70% 감소', 1),
(2, 1, '역할 기반 접근 제어', '부서 단위 권한을 포함한 체계적인 RBAC 구현', NULL, 2),
(3, 2, '실시간 멀티플레이어', 'WebSocket 기반 실시간 게임 세션으로 동시 접속 플레이어 지원', '최대 8명', 1),
(4, 2, '오디오 스트리밍', '프리로딩을 활용한 효율적인 음악 클립 스트리밍으로 끊김 없는 게임 플레이 제공', NULL, 2),
(5, 3, '주문 처리 파이프라인', '장바구니부터 배송 추적까지 엔드투엔드 주문 관리', NULL, 1),
(6, 3, '상품 검색', '필터링 및 카테고리 탐색을 지원하는 전문 검색', '응답 200ms 이내', 2);
