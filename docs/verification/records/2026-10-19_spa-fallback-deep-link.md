# 상세 URL 직접 접근 404 — SPA 경로 fallback 추가 (O-012)
- 일자: 2026-10-19
- 유형: 버그
- 우선순위: P1 (포트폴리오 링크 공유·새로고침 동선. 판정·SSE·API 는 무관)
- 판정: **미검증 상태** — 이 PC(JDK 8·11)에서 컴파일·테스트 불가. 개발자가 JDK 17 PC 에서 실행 후 갱신

## 1. 요청과 목적
- 사용자가 원한 것: `/api/**` 와 정적 파일을 제외한 경로를 `index.html` 로 넘기는 "컨트롤러" 추가. 테스트는 본인이 JDK 17 PC 에서 실행.
- 개발자 확인 결과(결정 사항): 운영에서 **F5 로 404 재현 확인**(본인). 수정 진행 결정.
- 진행 중 둔 가정:
  - "컨트롤러" 대신 **정적 리소스 핸들러의 resolver** 로 구현했다. 컨트롤러 매핑은 리소스 핸들러보다 우선이라 catch-all 컨트롤러는 `/assets/*.js`·`/thumbnails/**` 까지 가로채고, Spring 6 `PathPattern` 은 `/**/{path}` 처럼 중간 `**` 를 허용하지 않아 "확장자 있는 경로 제외" 를 컨트롤러 패턴으로 쓸 수 없다. 효과(요청한 경로 → `index.html`)는 동일하다.

### 이상 제보 대응 (전역 §8)
- **① 오류인지** — 기대: `https://kyuhyeong.com/projects/song-quiz` 직접 접근 시 상세 화면 / 관찰: Spring Whitelabel(브라우저)·JSON(curl) 404. 유형: **결함**(설계 누락). 증거: curl 3회 404 JSON + 본인 F5 재현. "앱 내 이동은 정상" 은 React Router 가 서버 요청을 안 보내기 때문이며 반례가 아니다.
- **② 오류파악** — 사실: 백엔드에 `/api/**` 외 매핑 없음(`WebConfig` 는 CORS 만), git 이력에 포워딩이 있었던 적 없음, 응답이 nginx 가 아닌 Spring 의 것(JSON 에러 본문) → nginx 도 경로를 바꾸지 않음. **원인(확인됨, 2계층: 코드 + 운영 응답)**: `BrowserRouter` + 서버 fallback 부재. 이번 디자인 변경과 무관하게 처음부터 존재.
- **③ 소스수정** — `[원인해결]` `SpaFallbackConfig` 추가. `[방어]` `/api/**` 미매핑 경로는 HTML 로 위장하지 않고 404 유지(배포 health 폴링·UptimeRobot 오판 방지).

## 2. Acceptance Criteria
| # | 구분 | 조건 | 상태 | 근거 |
|---|---|---|---|---|
| 1 | 정상 | `/projects/{slug}` 를 직접 열거나 새로고침하면 상세 화면이 뜬다(서버가 `index.html` 200) | 🙋 | `SpaFallbackTest#상세_URL_을_직접_열면_index_html_을_돌려준다` + 배포 후 F5 |
| 2 | 정상 | 루트 `/` 와 더 깊은 경로도 `index.html` | 🙋 | `#깊은_경로와_루트도_index_html_이다` |
| 3 | 경계 | 실제 정적 파일(`/assets/*.js`, `/thumbnails/**`)은 그 파일을 그대로 준다(가로채지 않음) | 🙋 | `#실제_정적_파일은_index_가_아니라_그_파일이다` + 배포 후 이미지 6개 200 |
| 4 | 예외 | 없는 파일 경로(확장자 있음)는 404 유지 | 🙋 | `#없는_파일_경로는_404_로_남긴다` |
| 5 | 노출 | `/api/**` 의 미매핑·미존재 경로는 200 HTML 로 위장하지 않고 404 | 🙋 | `#컨트롤러에_없는_API_경로는_HTML_로_위장하지_않고_404_다` |
| 6 | 연쇄 | `/api/monitoring/health/self` 200 · 배포 워크플로 health 폴링 영향 없음 | 🙋 | 배포 후 curl |
| 7 | 정상 | 기존 26건 테스트 통과(회귀) | 🙋 | `./gradlew test` |

## 3. 변경 사항
- `backend/src/main/java/com/kyuhyeong/dashboard/config/SpaFallbackConfig.java` — 신규. `/**` 리소스 핸들러 + `PathResourceResolver` 확장: 파일 있으면 파일, `/api/**` 또는 확장자 있으면 null(404), 그 외 `classpath:/static/index.html`
- `backend/src/test/java/com/kyuhyeong/dashboard/config/SpaFallbackTest.java` — 신규 6건(`@SpringBootTest` + `MockMvc`, dev 프로파일)
- `backend/src/test/resources/static/index.html`, `static/assets/app.js` — 테스트 전용 더미(운영 `static/` 은 Dockerfile 이 Vite dist 로 채움)
- `CLAUDE.md` — 단일 아티팩트 구조에 fallback 규칙, 테스트 건수 32
- DB·설정 변경: 없음

## 4. 영향 범위 분석
- 검색한 호출처와 결과: `addResourceHandlers` 구현 다른 곳 없음(`WebConfig` 는 CORS 만). 이 `/**` 등록으로 Boot 기본 정적 핸들러는 생략됨 — 기본과 동일한 `classpath:/static/` 만 쓰므로 운영 jar 의 `assets/`·`thumbnails/`·`favicon.svg` 서빙 경로 동일. `spring.web.resources.*` 설정은 없어 캐시 헤더 차이 없음
- 영향 받는 화면/기능: 모든 비-API GET. API·SSE·health·로그 경로는 컨트롤러가 먼저 받으므로 무관

## 5. 실행한 검증
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 빌드 | `backend/` `./gradlew build` | **미실행 — JDK 17 없음** | 🙋 |
| Unit/Integration | `./gradlew test --tests "SpaFallbackTest"` | 미실행 | 🙋 |
| 전체 회귀 | `./gradlew test` (32건 기대) | 미실행 | 🙋 |
| 사용자 시나리오 | 아래 §6 | 배포 후 | 🙋 |

## 6. 수동 확인 시나리오
1. [행동] JDK 17 PC 에서 `backend/` `./gradlew test`
   [기대 결과] 32 passed (기존 26 + SpaFallbackTest 6)
   - 결과: ☐ 통과 ☐ 실패
2. [행동] push → 배포 완료 후 아래
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" https://kyuhyeong.com/projects/song-quiz          # 200
   curl -s -o /dev/null -w "%{http_code}\n" https://kyuhyeong.com/api/nope                     # 404
   curl -s -o /dev/null -w "%{http_code}\n" https://kyuhyeong.com/thumbnails/missing.png       # 404
   curl -s -o /dev/null -w "%{http_code}\n" https://kyuhyeong.com/thumbnails/song-quiz/01.png  # 200
   curl -s -o /dev/null -w "%{http_code}\n" https://kyuhyeong.com/api/monitoring/health/self   # 200
   ```
   브라우저에서 상세 페이지 F5 → 상세 화면 유지
   - 결과: ☐ 통과 ☐ 실패

## 7. checklist 점검
- 점검함: 호출처(리소스 핸들러 중복) 검색, `/api/**` 위장 방지, 정적 파일 가로채기 방지, 테스트 더미가 운영 jar 에 안 들어감(test resources), CLAUDE.md 동기화
- 해당 없음: DB·설정, 인증

## 8. 발견된 문제와 조치
- 없음(이 기록 자체가 2026-10-19 배포 Smoke 부수 발견 O-012 의 조치)

## 9. 미검증 영역과 남은 위험
- 전부 🙋 — 컴파일조차 이 PC 에서 못 했다. `PathResourceResolver#getResource(String, Resource)` 오버라이드 시그니처가 Spring 6.1 과 맞는지 컴파일로 확인 필요
- 운영 jar 안(BOOT-INF/classes/static/)에서 `ClassPathResource("/static/index.html").exists()` 가 true 인지는 배포 후 §6-2 로만 확인

## 10. Regression 등록
- regression-list.md 추가 ID: R-016
