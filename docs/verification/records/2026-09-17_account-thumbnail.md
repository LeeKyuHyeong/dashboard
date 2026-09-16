# Account 카드 썸네일 추가
- 일자: 2026-09-17
- 유형: 수정
- 우선순위: P3 (이미지·시드 값)
- 판정: 조건부 (운영 DB UPDATE 와 화면 확인은 🙋)

## 1. 요청과 목적
- 사용자가 원한 것: NHN 지원용으로 찍은 account 앱 캡처 3장을 dashboard 의 Account 카드 썸네일로 사용
- 개발자 확인 결과(결정 사항): 캡처 원본은 `career/20-applications/2026-09-nhn-payco/img/image-1789571669121.png`·`…690754.png`·`…694260.png`(모바일 세로 화면)
- 진행 중 둔 가정: 카드 썸네일 영역이 `height:160px; object-fit:cover` 라 세로 캡처 1장을 그대로 쓰면 가운데 띠만 보인다 → 기존 썸네일과 같은 1917×911 캔버스에 3장을 가로로 합성(중앙 760px 높이 안에 배치, 밝은 회색 배경·둥근 모서리·그림자). 캡처 하단 8px(창 테두리 픽셀)은 잘라냄

## 2. Acceptance Criteria
| # | 구분 | 조건 | 상태 | 근거 |
|---|---|---|---|---|
| 1 | 정상 | 메인 프로젝트 탭의 Account 카드에 세 화면(홈·영수증·영수증 확인)이 보인다 | 🙋 | 배포 + 운영 DB UPDATE 후 브라우저 확인 |
| 2 | 정상 | 새 썸네일이 jar 정적 자원으로 서빙된다(`/thumbnails/account.png` 200) | 🙋 | 배포 후 GET |
| 3 | 연쇄 | dev(H2) 시드가 새 값으로 적재된다 | ✅ | `./gradlew test` contextLoads 통과(`data-h2.sql` 실행) |
| 4 | 연쇄 | 다른 카드(ITSM·Song Quiz)는 변경 없음 | ✅ | 시드 diff 가 account 행 1곳씩 |

## 3. 변경 사항
- `frontend/public/thumbnails/account.png` — 신규(1917×911, 134KB). 생성 스크립트는 스크래치(`make_account_thumb.py`, Pillow) — 저장소에 두지 않음
- `backend/src/main/resources/data-mariadb.sql`, `data-h2.sql` — account 행 `thumbnail_url` NULL → `/thumbnails/account.png`
- DB·설정 변경: 운영 DB 는 시드를 읽지 않으므로(`spring.sql.init.mode: never`) 아래 6번 UPDATE 를 본인이 실행

## 4. 영향 범위 분석
- `thumbnailUrl` 사용처: `frontend/src/components/ProjectCard.jsx:20-21` 1곳(있으면 `<img>`, 없으면 placeholder). 상세 페이지는 썸네일을 쓰지 않음
- 영향 받는 화면: 메인 프로젝트 탭 Account 카드만

## 5. 실행한 검증
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 이미지 | 생성 결과를 열어 확인 | 3화면 배치·잘림 없음 | ✅ |
| 전체 회귀 | `backend/` `./gradlew test --no-daemon` | 14 tests (1+5+8), 0 failures | ✅ |
| 빌드(결합) | 루트 `Dockerfile` | CI 에서 수행 | 🙋 (deploy.yml 결과) |
| 사용자 시나리오 | 아래 6번 | 확인 대기 | 🙋 |

## 6. 수동 확인 시나리오
1. [전제] `main` push 로 배포 완료(Actions 성공, `/api/monitoring/health/self` 200)
2. [행동] 운영 DB: `UPDATE project SET thumbnail_url='/thumbnails/account.png', updated_at=NOW() WHERE slug='account';`
3. [행동] 브라우저(브라우저 UA)로 https://kyuhyeong.com 프로젝트 탭
4. [기대 결과] Account 카드 상단 160px 에 세 화면이 나란히 보이고 상하 잘림이 어색하지 않음. `https://kyuhyeong.com/thumbnails/account.png` 200
- 결과: ☐ 통과 ☐ 실패

## 7. checklist 점검
- 점검함: 영향 범위(사용처 1곳), 운영 데이터 주인은 DB(시드는 기준값)
- 해당 없음: 권한·입력 검증·트랜잭션

## 8. 발견된 문제와 조치
- 없음

## 9. 미검증 영역과 남은 위험
- 카드 폭이 넓은 화면(3열 그리드)에서 `cover` 크롭 정도는 실제 화면에서만 확인 가능 → 시나리오 4

## 10. Regression 등록
- 없음
