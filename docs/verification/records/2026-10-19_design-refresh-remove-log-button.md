# 디자인 개편(썸네일 → 상세 갤러리, 포트폴리오 헤더) + 로그 버튼 제거
- 일자: 2026-10-19
- 유형: 수정
- 우선순위: P1 (메인·상세 렌더는 P0 동선이나 판정·SSE 로직은 손대지 않음) / 백엔드 칼럼 추가는 P2
- 판정: **조건부** — 프론트는 로컬 mock 으로 전부 ✅, 백엔드(JDK 17 필요)와 운영 DB 반영은 🙋

## 1. 요청과 목적
- 사용자가 원한 것: ① 디자인 개선 ② 카드의 '로그' 버튼 제외.
- 개발자 확인 결과(결정 사항):
  - 로그: **프론트 전부 제거**(버튼·LogModal·fetchLogs·모달 CSS). 백엔드 `/api/monitoring/logs` 와 허용 목록 검사, nginx 차단은 유지.
  - 디자인: 화면을 먼저 보고 결정 → "카드 썸네일은 작아서 무슨 화면인지 모르겠다. **카드에서 썸네일 박스를 없애고, 상세에서 최대 3장**을 보여 달라. 포트폴리오 느낌 강화."
- 진행 중 둔 가정:
  - 3장을 담을 자리로 `project.screenshot_urls`(JSON 배열) 칼럼을 **추가**했다(`tech_stack` 과 같은 방식). 스키마 변경은 전역 §3 결정 항목이라 **개발자 확인 필요** — 대안은 `project_screenshot` 테이블 또는 프론트 정적 매핑.
  - 헤더 문구(`kyuhyeong.com` / 태그라인 / GitHub 링크)는 `MainPage.jsx` 의 `SITE` 상수에 임시로 넣었다. 실명·문구는 개발자가 정한다.
  - Account 3장은 기존 합성 썸네일(`account.png`, 화면 3장 가로 합성)을 **잘라서** 만들었다. Song Quiz 3장(메인 메뉴·관리자·멀티 방)은 **본인이 직접 캡처해 `frontend/public/thumbnails/song-quiz/01~03.png` 에 넣기로 결정** — 브라우저 패널 캡처는 가로 640px 이하라 포트폴리오용으로 부족했고, 페이지에서 로컬로 고해상도 캡처를 보내는 방식은 권한 분류기가 막아 시도하지 않았다. 시드·mock 은 그 경로로 미리 맞춰 두었다(O-011).
  - 이 PC(회사)는 JDK 8·11 만 있어 **백엔드 컴파일·테스트를 실행하지 못했다**.

## 2. Acceptance Criteria
| # | 구분 | 조건 | 상태 | 근거 |
|---|---|---|---|---|
| 1 | 정상 | 메인 카드에 '로그' 버튼이 없고 눌러서 열리던 모달도 없다 | ✅ | mock 화면 확인, `grep -rn "LogModal\|fetchLogs" src` 0건 |
| 2 | 정상 | 메인 카드에 썸네일 박스가 없고 이름·상태 뱃지·설명·태그·Docker/가동시간·Live/GitHub·`상세 →` 가 보인다 | ✅ | mock 화면(1280px) 확인 |
| 3 | 정상 | 상단 헤더에 사이트명·태그라인·GitHub 링크·실시간 표시 | ✅ | 〃 |
| 4 | 정상 | 상세 `화면` 섹션에 `screenshotUrls` 를 최대 3장 그린다 | ✅ | account(3장)·song-quiz(3장: 멀티 방·메인 메뉴·관리자, 본인 캡처) mock 확인 — 6장 모두 naturalWidth > 0 |
| 5 | 정상 | 화면 클릭 → 원본 확대, →/← 로 이동, ESC 로 닫힘 | ✅ | `role=dialog` 생성 확인 → ArrowRight 후 `3 / 3` → Escape 후 dialog 없음 |
| 6 | 예외 | `screenshotUrls` 가 비면 `thumbnailUrl` 1장으로 대체, 둘 다 없으면 섹션 숨김 | ⬜ | 코드 경로(`screenshotsOf`)만 확인. mock 으로 빈 배열 케이스 미실행 |
| 7 | 경계 | 4장 이상이면 앞 3장만 | ⬜ | `slice(0, 3)` — 데이터 없음 |
| 8 | 정상 | 375px 에서 헤더 세로 배치·카드 1열·갤러리 1열, 가로 스크롤 없음 | ✅ | mobile 프리셋 캡처 |
| 9 | 연쇄 | `GET /api/projects`·`/api/projects/{slug}` 가 `screenshotUrls` 배열을 돌려준다(null·빈 문자열·파싱 실패는 `[]`) | 🙋 | JDK 17 PC 에서 `./gradlew test` + dev 기동 후 curl |
| 10 | 연쇄 | 운영 DB 에 `screenshot_urls` 칼럼이 없으면 JPA SELECT 가 실패해 **프로젝트 API 전체가 500** 이 된다 → 배포 전 ALTER 필수 | 🙋 | 아래 §6-1 |
| 11 | 정상 | `npm run lint` 0 error, `npm run build` 성공 | ✅ | §5 |
| 12 | 노출 | 로그 경로 노출 범위 변화 없음(프론트 호출처만 사라짐) | ✅ | 백엔드·nginx 미변경 |

## 3. 변경 사항
- `frontend/src/components/ProjectCard.jsx` — 썸네일·로그 버튼·LogModal 제거, 상태를 `dl` 로, 하단에 Live/GitHub 아이콘 링크 + `상세 →`
- `frontend/src/components/LogModal.jsx` — 삭제
- `frontend/src/api/index.js` — `fetchLogs` 삭제
- `frontend/src/components/ScreenshotGallery.jsx` — 신규. 최대 3장 + 라이트박스(←/→/ESC)
- `frontend/src/components/icons.jsx` — 신규. ExternalLink·GitHub SVG 공용화(`ProjectHeader` 도 사용)
- `frontend/src/pages/MainPage.jsx` — 헤더를 `SITE` 상수(사이트명·태그라인·GitHub)로
- `frontend/src/pages/ProjectDetailPage.jsx` — 갤러리 삽입 + `screenshotsOf` 대체 규칙. **[개선]** 응답을 `{slug, project, error}` 로 묶어 effect 안의 동기 setState 제거(`react-hooks/set-state-in-effect` 기존 lint 오류 해소) — 부수 효과로 slug 전환 뒤 늦게 온 이전 응답이 화면에 오르지 않는다(R-015)
- `frontend/src/index.css` — 모달·`.btn--logs`·`.service-card__*`·썸네일 CSS 삭제, 헤더/카드/`icon-link`/갤러리/라이트박스 추가, 프로젝트 그리드 `minmax(320px, 520px)`. 성과 수치 뱃지(`.achievement-card__metric`) 모노스페이스 → 본문 폰트(사용자 피드백)
- `frontend/public/thumbnails/account/01~03.png` — 신규(합성본 분할)
- `frontend/public/thumbnails/song-quiz/01~03.png` — 신규(본인 캡처: 멀티 방 760×889 · 메인 메뉴 1031×911 · 관리자 1899×910)
- `backend/.../domain/Project.java`, `dto/ProjectListDto.java`, `dto/ProjectDetailDto.java` — `screenshotUrls`(JSON 문자열 → `List<String>`) 추가, `parseStringList` 공용화
- `backend/src/main/resources/schema.sql` — 칼럼 + `ALTER ... IF NOT EXISTS`
- `CLAUDE.md` — 화면 설계·ERD·컴포넌트 트리·관리 방침·로그 서술 갱신
- `.claude/launch.json` — 신규(로컬 Vite 미리보기용). 커밋 여부는 개발자 판단
- DB·설정 변경: **`project.screenshot_urls TEXT` 추가**. 시드 `data-h2.sql`·`data-mariadb.sql` 두 벌에 값 반영(O-006 규칙). `application.yml` 변경 없음

## 4. 영향 범위 분석
- 검색한 호출처와 결과:
  - `LogModal` → `ProjectCard` 뿐 / `fetchLogs` → `LogModal` 뿐 / `.modal*`·`.btn--logs` → `LogModal` 뿐 → 모두 함께 삭제
  - `.service-card__*` → `ProjectCard` 뿐(새 마크업은 `project-card__status`) → 삭제
  - `thumbnailUrl` 프론트 소비처 → `ProjectCard`(제거)·상세 대체 규칙(신규). API 응답에는 그대로 남김
  - `new ProjectListDto/ProjectDetailDto` 직접 호출 → 테스트 포함 0건(`from()` 만 사용) → 생성자 인자 추가가 깨뜨리는 코드 없음(컴파일로 재확인 필요 🙋)
- 영향 받는 화면/기능: 메인 프로젝트 탭, 상세 페이지. 서버 메트릭 탭·SSE·판정 로직·로그 API·nginx 는 무관

## 5. 실행한 검증
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 프론트 lint | `frontend/` `npx eslint .` | 0 problems (작업 전엔 `ProjectDetailPage` set-state-in-effect 1 error — 원본 HEAD 에서도 동일) | ✅ |
| 프론트 빌드 | `frontend/` `npm run build` | `✓ built`, 40 modules | ✅ |
| 화면(mock) | Vite :3000 + scratchpad mock 백엔드 :8080(`/api/projects`, `/api/projects/{slug}`, SSE) | 메인·상세(account 3장/song-quiz 1장)·라이트박스·375px | ✅ |
| 백엔드 빌드·Unit | `backend/` `./gradlew test` | **미실행 — 이 PC 에 JDK 17 없음(8·11)** | 🙋 |
| 전체 회귀(백엔드 26건) | 〃 | 〃 | 🙋 |
| 사용자 시나리오(운영) | 아래 §6 | 배포 후 | 🙋 |

## 6. 수동 확인 시나리오
### 6-1. 운영 DB 선반영 (push **전에** — 칼럼이 없으면 새 컨테이너의 프로젝트 API 가 500)
1. [전제] dashboard-db 컨테이너에 접속
2. [행동]
   ```sql
   ALTER TABLE project ADD COLUMN IF NOT EXISTS screenshot_urls TEXT;
   UPDATE project SET screenshot_urls = '["/thumbnails/song-quiz/01.png", "/thumbnails/song-quiz/02.png", "/thumbnails/song-quiz/03.png"]' WHERE slug = 'song-quiz';
   UPDATE project SET screenshot_urls = '["/thumbnails/account/01.png", "/thumbnails/account/02.png", "/thumbnails/account/03.png"]' WHERE slug = 'account';
   ```
3. [기대 결과] `SELECT slug, screenshot_urls FROM project;` 에 두 행이 채워짐. itsm 은 NULL(숨김이라 무관)
- 결과: ☐ 통과 ☐ 실패

### 6-2. 백엔드 테스트 (JDK 17 있는 PC)
1. [행동] `backend/` `./gradlew test`
2. [기대 결과] 26 passed(기존 건수). `dev` 기동 후 `curl localhost:8080/api/projects/account` 에 `"screenshotUrls":["/thumbnails/account/01.png",…]`
- 결과: ☐ 통과 ☐ 실패

### 6-3. 배포 후 Smoke
1. [행동] `https://kyuhyeong.com/` 접속
2. [기대 결과] 헤더(사이트명·태그라인·GitHub·실시간), 카드 2장에 썸네일·로그 버튼 없음, 상세 account·song-quiz 각 화면 3장·클릭 확대, `/api/monitoring/health/self` 200
- 결과: ☐ 통과 ☐ 실패

## 7. checklist 점검
- 점검함: 호출처 검색(§4), 삭제 코드의 CSS·API 잔재, 시드 두 벌 동시 반영, CLAUDE.md 동기화, 운영 프로파일 `init.mode` 미변경, 반응형, 접근성(갤러리 버튼 aria-label·dialog role)
- 해당 없음: 인증·권한(로그인 없음), 외부 연동 Mock(변경 없음)

## 8. 발견된 문제와 조치
- `ProjectDetailPage.jsx` 가 작업 전부터 lint 를 통과하지 못했다(`eslint-plugin-react-hooks` v7 `set-state-in-effect`). 원인: effect 본문에서 `setLoading(true)` 동기 호출. 조치: §3 **[개선]**. 추가한 테스트: 없음(프론트 테스트 러너 없음) → R-015 수동 항목

## 9. 미검증 영역과 남은 위험
- 백엔드 컴파일·테스트(JDK 17 없음) — 6-2 로 확인
- 운영 DB 칼럼 미반영 상태로 push 하면 프로젝트 API 500 — **6-1 을 먼저** (O-010)
- AC 6·7 대체·절단 규칙은 코드로만 확인

## 10. Regression 등록
- regression-list.md 추가 ID: R-015
