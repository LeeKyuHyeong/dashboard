# 숨김 프로젝트 노출 가드에 자동 테스트 추가 (O-005)
- 일자: 2026-09-26
- 유형: 수정 (테스트 보강)
- 우선순위: P1 (노출 가드 — 숨긴 프로젝트가 새면 의도치 않은 공개)
- 판정: 수용 가능

## 1. 요청과 목적
- 사용자가 원한 것: O-005 진행.
- 배경: 2026-09-17 에 itsm 카드를 숨겼을 때(`visible=FALSE`), 그 가드가 동작한다는 근거는
  소스 한 줄(`ProjectController.java:21`·`:30`)과 운영 DB 조회뿐이었다.
  노출 가드인데 자동 테스트가 없어 2026-09-25 정비 때 O-005 로 등록했다.
- 진행 중 둔 가정: 없음. 운영 코드는 건드리지 않고 테스트만 추가한다(기존 동작을 고정하는 작업).

## 2. Acceptance Criteria
| # | 구분 | 조건 | 상태 | 근거 |
|---|---|---|---|---|
| 1 | 노출 | 숨긴 프로젝트가 목록(`GET /api/projects`)에 나오지 않는다 | ✅ | `#숨긴_프로젝트는_목록에_나오지_않는다` |
| 2 | 권한 | 숨긴 프로젝트는 slug 를 알아도 상세로 읽히지 않는다(404) | ✅ | `#숨긴_프로젝트는_slug_를_알아도_상세로_읽히지_않는다` |
| 3 | 정상 | 목록이 보이는 것만 `sortOrder` 오름차순으로 나온다 | ✅ | `#목록은_보이는_것만_sortOrder_순서로_돌려준다` |
| 4 | 정상 | 보이는 프로젝트는 상세로 읽힌다(가드가 정상 경로를 막지 않는다) | ✅ | `#보이는_프로젝트는_상세로_읽힌다` |
| 5 | 예외 | 없는 slug 는 404 | ✅ | `#없는_slug_는_404` |
| 6 | 경계 | `visible` 이 NULL 이면 목록·상세 둘 다에서 빠진다(NPE 없음) | ✅ | `#visible_이_NULL_이면_목록에도_상세에도_나오지_않는다` |
| 7 | 연쇄 | 운영 코드는 변경하지 않는다 | ✅ | `git diff -- backend/src/main` 빈 출력 |

## 3. 변경 사항
- `backend/src/test/java/com/kyuhyeong/dashboard/controller/ProjectVisibilityTest.java` — 신규, 6건
- `docs/verification/open-issues.md` — O-005 CLOSED
- `docs/verification/regression-list.md` — R-012 수동 → 자동 승격, R-013·R-014 추가
- 운영 코드·DB·설정 변경: **없음**

## 4. 영향 범위 분석
- 테스트 추가만이므로 런타임 영향 없음.
- 기존 테스트 수정·삭제 없음(26건 그대로, 32건으로 증가).

## 5. 왜 mock 이 아니라 @DataJpaTest 인가
목록 필터는 **메서드 이름에만** 있다 — `findAllByVisibleTrueOrderBySortOrderAsc`.
Spring Data 가 런타임에 이 이름을 파싱해 SQL 을 만든다. 저장소를 mock 으로 바꾸면
컨트롤러가 "그 메서드를 부른다"는 것만 확인되고, **이름이 틀려도 테스트는 통과한다.**
가드가 실제로 거르는지 보려면 진짜 H2 + 진짜 파생 쿼리가 필요하다.

그래서 `@DataJpaTest` 안에서 주입받은 실제 저장소로 `new ProjectController(...)` 를 만들었다.
확인하려는 것이 "두 경로 모두 막혔는가" 이므로, 저장소와 컨트롤러를 함께 통과시켜야 의미가 있다.

## 6. 실행한 검증
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 신규 테스트 | `./gradlew test --tests '*ProjectVisibilityTest*'` | 6 passed | ✅ |
| **가드 제거 후 실패 확인** | `findAllByVisibleTrueOrderBySortOrderAsc`→`findAll`, 상세의 `.filter(...)` 제거 후 재실행 | **6건 중 4건 FAILED** — 노출 관련 4건만 정확히 실패, 정상 경로 2건은 통과 유지 | ✅ |
| 원복 | `git checkout -- ProjectController.java` | `git diff -- backend/src/main` 빈 출력 | ✅ |
| 빌드 + 전체 회귀 | `backend/` `./gradlew test --no-daemon` | **32 tests, 0 failures, 0 errors** (26 + 신규 6) | ✅ |

가드를 부숴 실패를 본 뒤 원복했다. 실패를 확인하지 않은 테스트는 무엇도 지키지 못한다.

## 7. checklist 점검
- 점검함: 테스트가 실제로 대상을 검증하는지(가드 제거 실패 확인), 검증 대상을 mock 으로 대체하지 않았는지, 운영 코드 무변경, 기존 테스트 무수정
- 해당 없음: DB 마이그레이션·권한·동시성·화면

## 8. 발견된 문제와 조치
- **문제**: 첫 실행에서 6건 전부 `SQLGrammarException` — `Table "PROJECT" not found (this database is empty)`.
- **원인**: `application.yml` 기본 프로파일이 `hibernate.dialect: MariaDBDialect` 다.
  `@DataJpaTest` 는 데이터소스만 H2 로 바꾸고 dialect 는 그대로 둔다 →
  Hibernate 가 MariaDB 전용 DDL 을 내보내고 H2 가 거부해 **테이블이 하나도 만들어지지 않았다**.
  (종료 로그의 `alter table ... drop foreign key` 가 MariaDB 문법이라는 점이 단서)
- **조치**: `@TestPropertySource` 에 `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect` 를 명시.
  시드(`data-h2.sql`)도 `spring.sql.init.mode=never` 로 꺼서 고정값만 보게 했다.
- 운영 설정은 바꾸지 않았다 — 테스트 안에서만 덮어쓴다.

## 9. 미검증 영역과 남은 위험
- HTTP 계층(`@WebMvcTest`/MockMvc)은 거치지 않는다 — 컨트롤러 메서드를 직접 부른다.
  경로 매핑·직렬화가 아니라 **노출 가드**를 고정하는 것이 목적이고, 매핑은 운영에서 매일 쓰여 확인된다.
- MariaDB 와 H2 의 `visible = true` 해석 차이는 보지 않았다. 둘 다 표준 BOOLEAN 비교라 위험이 낮다고 판단했다.
- itsm 을 다시 공개할 때 운영 DB 와 시드를 함께 되돌려야 하는 것은 그대로 O-006 이다.

## 10. Regression 등록
- R-012 를 수동 → 자동으로 승격
- R-013 신규 — 목록이 보이는 것만 `sortOrder` 순으로 나온다
- R-014 신규 — `visible` 이 NULL 이면 양쪽에서 빠진다
