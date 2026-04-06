-- Insert projects (MariaDB)
INSERT IGNORE INTO project (id, name, slug, description, tech_stack, demo_url, github_url, thumbnail_url, sort_order, created_at, updated_at) VALUES
(1, 'ITSM', 'itsm', 'IT Service Management system built with Vue.js frontend and Spring Boot backend. Manages IT service requests, incidents, and changes.',
 '["Vue.js", "Spring Boot", "MariaDB", "Docker"]',
 'https://itsm.kyuhyeong.com', 'https://github.com/rbgud/itsm', NULL, 1, NOW(), NOW()),
(2, 'Song Quiz', 'song-quiz', 'Real-time multiplayer song guessing game. Players listen to short clips and compete to identify songs the fastest.',
 '["React", "Spring Boot", "WebSocket", "Redis", "Docker"]',
 'https://game.kyuhyeong.com', 'https://github.com/rbgud/song-quiz', NULL, 2, NOW(), NOW()),
(3, 'KH Shop', 'kh-shop', 'Full-featured e-commerce platform with product catalog, shopping cart, order management, and payment integration.',
 '["React", "Spring Boot", "MariaDB", "Redis", "Docker"]',
 'https://shop.kyuhyeong.com', 'https://github.com/rbgud/kh-shop', NULL, 3, NOW(), NOW());

-- Insert achievements
INSERT IGNORE INTO project_achievement (id, project_id, title, description, metric_value, sort_order, created_at) VALUES
(1, 1, 'Service Request Automation', 'Automated IT service request workflow reducing manual processing time', '70% reduction', 1, NOW()),
(2, 1, 'Role-Based Access Control', 'Implemented comprehensive RBAC with department-level permissions', NULL, 2, NOW()),
(3, 2, 'Real-time Multiplayer', 'WebSocket-based real-time game sessions supporting concurrent players', 'Up to 8 players', 1, NOW()),
(4, 2, 'Audio Streaming', 'Efficient audio clip streaming with preloading for seamless gameplay', NULL, 2, NOW()),
(5, 3, 'Order Processing Pipeline', 'End-to-end order management from cart to delivery tracking', NULL, 1, NOW()),
(6, 3, 'Product Search', 'Full-text search with filtering and category navigation', '< 200ms response', 2, NOW());
