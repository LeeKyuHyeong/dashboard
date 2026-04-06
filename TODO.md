# Portfolio Dashboard - 개발 TODO

## Phase 1: 프로젝트 초기 설정
- [x] Spring Boot 프로젝트 생성 (Gradle, Java 17)
- [x] 의존성 추가: Spring Web, Spring Data JPA, MariaDB, OSHI, Docker Java
- [x] application.yml 설정 (DB, 모니터링 서비스 매핑)
- [x] React 프론트엔드 프로젝트 생성 (Vite)
- [x] 프론트엔드 의존성: Zustand, Recharts, React Router

## Phase 2: Backend - 도메인 & DB
- [x] Project 엔티티 + Repository
- [x] ProjectAchievement 엔티티 + Repository
- [x] DB 스키마 초기화 (schema.sql / data.sql)

## Phase 3: Backend - 프로젝트 API
- [x] GET /api/projects — 프로젝트 목록
- [x] GET /api/projects/{slug} — 프로젝트 상세 + achievements

## Phase 4: Backend - 모니터링
- [x] ServiceStatus / ServerMetric 모델 (인메모리)
- [x] MonitoringProperties (yaml 매핑)
- [x] HealthCheckService — HTTP health check + Docker 상태 수집
- [x] ServerMetricService — OSHI로 CPU/Memory/Disk 수집
- [x] MonitoringScheduler — 10초 주기 스케줄러
- [x] SSE /api/monitoring/stream 엔드포인트
- [x] GET /api/monitoring/logs/{containerName} 엔드포인트

## Phase 5: Frontend - 기본 구조
- [x] React Router 설정 (/, /projects/:slug)
- [x] useMonitoringStore (Zustand)
- [x] useSSE 커스텀 훅
- [x] API 호출 유틸

## Phase 6: Frontend - 메인 페이지
- [x] ServiceCard 컴포넌트 (UP/DOWN, 응답시간, Docker 상태, 업타임)
- [x] MetricCard 컴포넌트 (CPU, Memory, Disk + Recharts)
- [x] ProjectCard 컴포넌트 (썸네일, 이름, 설명, 태그)
- [x] LogModal 컴포넌트
- [x] MainPage 조합

## Phase 7: Frontend - 상세 페이지
- [x] ProjectHeader 컴포넌트
- [x] TechStackTags 컴포넌트
- [x] AchievementCard 컴포넌트
- [x] ProjectDetailPage 조합

## Phase 8: 빌드 & 배포
- [x] Frontend 빌드 → Spring Boot static 리소스로 통합
- [x] Dockerfile 작성
- [x] docker-compose.yml 작성
