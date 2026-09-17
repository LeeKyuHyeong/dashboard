# itsm 카드 시드를 운영 DB 에 맞춤 (숨김·github_url NULL)
- 일자: 2026-09-17
- 유형: 수정
- 우선순위: P2 (콘텐츠 DB 시드)
- 판정: 수용 가능 (운영 DB 는 본인이 먼저 반영·조회 완료, 시드는 기준값 정합만)

## 1. 요청과 목적
- 사용자가 원한 것: itsm 카드를 운영 DB 와 같이 **숨김(`visible=FALSE`)·`github_url` NULL** 로 시드에도 맞춘다
- 배경: `7a339e0`(09-16) 에서 시드는 itsm 을 `visible=TRUE`·URL 복원으로 바꿨으나, 운영 DB 는 숨김을 유지했고 09-17 본인이 `github_url` 을 NULL 로 변경. itsm 앱에 시연 계정 로그인 실패 등 정리할 오류가 남아 있어 그 전까지 포트폴리오에 노출하지 않기로 함(본인 결정)
- 시드와 운영이 어긋난 채 두면 DB 재구축 시 itsm 카드가 노출됨

## 2. Acceptance Criteria
| # | 구분 | 조건 | 상태 | 근거 |
|---|---|---|---|---|
| 1 | 정상 | 시드 두 벌(H2·MariaDB)의 id 1 이 `visible=FALSE`·`github_url=NULL` 이다 | ✅ | diff 2파일 각 1행 |
| 2 | 노출 | 운영 DB id 1 이 `visible=0`·`github_url=NULL` 이다 | ✅ | 본인 조회 결과(09-17, `SELECT id, slug, visible, github_url, thumbnail_url FROM project`) |
| 3 | 연쇄 | dev(H2) 시드가 오류 없이 적재된다 | ✅ | `./gradlew test` contextLoads 통과 |
| 4 | 연쇄 | 다른 카드(Song Quiz·Account)는 변경 없음 | ✅ | diff 가 id 1 행과 주석뿐 |
| 5 | 권한 | 숨긴 카드는 상세 API 로도 읽히지 않는다 | ✅ | `ProjectController.java:30` visible 필터(`bd95ec7`) — 코드 변경 없음 |

## 3. 변경 사항
- `backend/src/main/resources/data-h2.sql`, `data-mariadb.sql` — id 1 `github_url` → NULL, `visible` TRUE → FALSE, 주석 갱신
- DB·설정 변경: 없음 (운영 DB 는 `spring.sql.init.mode: never` 라 시드를 읽지 않음. 운영은 본인이 이미 반영)

## 4. 영향 범위 분석
- `visible` 사용처: 목록 `findAllByVisibleTrueOrderBySortOrderAsc`(`ProjectController.java:21`), 상세 필터(`:30`)
- `github_url` 사용처: 프론트 `ProjectHeader.jsx` — null 이면 GitHub 버튼 미렌더링

## 5. 실행한 검증
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 전체 회귀 | `backend/` `./gradlew test --no-daemon` (JDK 17) | 14 tests (1+5+8), 0 failures | ✅ |
| 운영 데이터 | 본인 조회 | id 1 `visible=0`, `github_url=NULL` | ✅ |
| 빌드(결합) | 루트 `Dockerfile` | 배포 시 CI | ⬜ (시드 값만 변경, 운영 미적용 파일) |

## 6. 수동 확인 시나리오
- 해당 없음 (운영 화면은 DB 가 이미 숨김 상태)

## 7. checklist 점검
- 점검함: 운영 데이터 주인은 DB(시드는 기준값), 노출 범위(목록·상세 둘 다 숨김)
- 해당 없음: 입력 검증·트랜잭션·동시성

## 8. 발견된 문제와 조치
- 시드(`7a339e0`)와 운영 DB 불일치 → 이 변경으로 해소

## 9. 미검증 영역과 남은 위험
- itsm 을 다시 공개할 때는 운영 DB UPDATE 와 시드를 **함께** 되돌릴 것

## 10. Regression 등록
- 없음
