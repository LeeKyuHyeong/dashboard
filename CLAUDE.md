# Portfolio Dashboard

## 프로젝트 개요

kyuhyeong.com 메인 사이트. 운영 중인 서비스들의 실시간 상태 + 프로젝트 소개를 한 눈에 보여주는 허브.
자기소개 섹션 없음 — 프로젝트가 곧 자기소개.

### 모니터링 대상 (3개)

- ITSM (Vue.js + Spring Boot)
- Song Quiz — 노래맞추기 게임
- KH Shop — 이커머스 플랫폼

### 도메인

- kyuhyeong.com → 이 프로젝트 (포트폴리오 대시보드)
- kiryong.com → Kiryong Tech 정적 홈페이지 (변경 없이 유지)

---

## 기술 스택

- **Backend**: Spring Boot, MariaDB, SSE (SseEmitter), OSHI, Spring Actuator, Docker Engine API
- **Frontend**: React, Zustand, Recharts, React Router
- **Infra**: Cafe24 VPS (모니터링 대상과 동일 서버), Docker 컨테이너 매핑은 yaml 설정

---

## 화면 설계

### 메인 페이지 (`/`)

1. **Service status** — 서비스별 카드 3개 (UP/DOWN 뱃지, 응답시간, Docker 상태, 업타임, 로그 보기 버튼)
2. **Server metrics** — CPU, Memory, Disk 메트릭 카드 3개
3. **Projects** — 프로젝트 카드 그리드 (썸네일 + 이름 + 설명 + 기술 태그), 클릭 시 상세 페이지 이동

### 상세 페이지 (`/projects/:slug`)

1. **ProjectHeader** — 뒤로가기 + 프로젝트명 + 설명 + Live demo/GitHub 링크 버튼
2. **TechStackTags** — 기술 스택 태그 목록 (DB JSON에서 조회)
3. **AchievementList** — 주요 성과 카드 리스트 (번호 + 제목 + 설명 + 선택적 정량 수치)

---

## ERD (2개 테이블)

모니터링 데이터는 DB에 저장하지 않음. 인메모리(ConcurrentHashMap)로 관리.

### project

| 칼럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| name | VARCHAR | 프로젝트명 |
| slug | VARCHAR | URL 경로 (/projects/song-quiz) |
| description | TEXT | 프로젝트 설명 |
| tech_stack | JSON | 기술 스택 배열 ["Spring Boot", "React", ...] |
| demo_url | VARCHAR | Live demo 링크 |
| github_url | VARCHAR | GitHub 레포 링크 |
| thumbnail_url | VARCHAR | 카드 썸네일 이미지 |
| sort_order | INT | 정렬 순서 |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### project_achievement

| 칼럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT PK | |
| project_id | BIGINT FK | project.id |
| title | VARCHAR | 성과 제목 |
| description | TEXT | 성과 설명 |
| metric_value | VARCHAR | 정량 수치 (선택) |
| sort_order | INT | 정렬 순서 |
| created_at | TIMESTAMP | |

---

## 모니터링 데이터 관리

DB 대신 인메모리로 관리. Spring 스케줄러가 주기적(10초)으로 수집하여 메모리에 갱신.

```
인메모리 구조:
- Map<String, ServiceStatus> serviceStatuses  (containerName → 상태)
- ServerMetric serverMetric                    (단일 객체)

ServiceStatus: { name, status, responseTimeMs, dockerStatus, uptimeSeconds, checkedAt }
ServerMetric:  { cpuUsage, cpuCores, memoryUsed/Total, diskUsed/Total, collectedAt }
```

---

## API (4개 엔드포인트, 조회 전용)

### 프로젝트 콘텐츠

```
GET /api/projects
→ 프로젝트 목록 (sort_order 순, achievements 미포함)

GET /api/projects/{slug}
→ 프로젝트 상세 + achievements 포함
```

### 모니터링

```
SSE /api/monitoring/stream
→ 단일 SSE 스트림, 서비스 상태 + 서버 리소스 통합
→ 연결 즉시 현재 상태를 첫 이벤트로 전송 (별도 snapshot API 불필요)
→ 이후 10초 간격으로 업데이트 푸시
  event: monitoring
  data: { services: [...], server: {...}, timestamp: "..." }
```

### Docker 로그

```
GET /api/monitoring/logs/{containerName}?tail=100
→ 특정 컨테이너의 최근 로그 조회 (tail 100줄)
→ 서비스 카드의 "로그 보기" 버튼 클릭 시 모달로 표시
```

---

## 프론트엔드 컴포넌트 구조

```
App (React Router)
├── MainPage (/)
│   ├── ServiceCard ×3 (UP/DOWN, 응답시간, Docker 상태, 업타임, 로그 버튼)
│   ├── MetricCard ×3 (CPU, Memory, Disk)
│   ├── ProjectCard ×3 (썸네일, 이름, 설명, 태그)
│   └── LogModal (로그 보기 클릭 시 표시)
│
└── ProjectDetailPage (/projects/:slug)
    ├── ProjectHeader (이름, 설명, demo/github 링크)
    ├── TechStackTags (JSON → 태그 리스트)
    └── AchievementCard ×N (제목, 설명, 수치)
```

### 상태 관리

- **useMonitoringStore** (Zustand) — SSE 실시간 데이터 (서비스 상태 + 서버 리소스). 유일한 글로벌 스토어.
- 프로젝트 데이터는 각 페이지 컴포넌트 내부 useEffect + useState로 fetch. 글로벌 스토어 불필요.

### Custom Hooks

- **useSSE** — EventSource 연결/해제/재연결 로직 캡슐화

### 데이터 흐름

1. 페이지 진입 → useSSE 훅이 `/api/monitoring/stream`에 연결
2. 서버가 즉시 현재 상태를 첫 이벤트로 전송 → useMonitoringStore에 저장
3. 이후 10초 간격으로 업데이트 이벤트 수신 → 같은 store 갱신
4. 프로젝트 데이터는 `GET /api/projects`(목록) / `GET /api/projects/{slug}`(상세)로 컴포넌트 로컬 state에 저장

---

## Docker 매핑 설정 (application.yml)

```yaml
monitoring:
  services:
    - name: ITSM
      projectSlug: itsm
      healthUrl: http://localhost:8081/actuator/health
      containerName: itsm-app
    - name: Song Quiz
      projectSlug: song-quiz
      healthUrl: http://localhost:8082/actuator/health
      containerName: songquiz-app
    - name: KH Shop
      projectSlug: kh-shop
      healthUrl: http://localhost:8083/actuator/health
      containerName: khshop-app
  checkIntervalSeconds: 10
```

---

## 프로젝트 관리 방침

- 프로젝트 콘텐츠(project, project_achievement)는 DB 직접 수정 (관리자 API 없음)
- 모니터링 데이터는 인메모리 관리 (DB 저장 안 함, 서버 재시작 시 초기화)
- Docker 컨테이너 매핑은 yaml 설정 파일로 관리
