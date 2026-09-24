# 검증 문서 체계 정비 (regression-list·open-issues 신설)
- 일자: 2026-09-25
- 유형: 수정 (문서·프로세스)
- 우선순위: P2
- 판정: 수용 가능

## 1. 요청과 목적
- 사용자가 원한 것: dashboard 의 검증 문서를 전역 규칙에 맞게 정비하고 푸시.
- 배경: 전역 `CLAUDE.md` §6 은 🙋·⬜ 항목을 `open-issues.md` 에, 버그 재현 테스트를 `regression-list.md` 에 등록하라고 한다.
  dashboard 에는 `README.md` 와 `records/` 만 있고 두 파일이 없었다.
  `records/2026-09-20_logs-allowlist-and-healthcheck.md` §10 이 직접 "이 저장소에는 regression-list 파일이 아직 없다 — 테스트 클래스 주석에 발견 경위를 남김" 이라고 적어 둔 상태였다.
- 진행 중 둔 가정: 없음. ID 체계(R-0xx·O-0xx)와 표 형식은 quiz 리포의 같은 파일을 따랐다(저장소별 독립 번호).

## 2. Acceptance Criteria
| # | 구분 | 조건 | 상태 | 근거 |
|---|---|---|---|---|
| 1 | 정상 | `regression-list.md` 가 기존 기록의 재현 테스트를 빠짐없이 담는다 | ✅ | R-001~R-012, 09-20 기록의 AC C1-1~C1-5·C2-2~C2-6 전부 대응 |
| 2 | 정상 | 인용한 테스트 메서드명이 실제 소스와 일치한다 | ✅ | §5 — 두 테스트 클래스의 메서드명을 소스에서 추출해 대조 |
| 3 | 정상 | `open-issues.md` 가 기존 기록의 🙋·⬜·§9 항목을 빠짐없이 담는다 | ✅ | O-001~O-008, 기록 3건의 §9 와 🙋 AC 전부 대응 |
| 4 | 정상 | 남은 🙋 가 실제로 열려 있는지 확인하고 닫을 것은 닫는다 | ✅ | O-001 운영 확인으로 닫음 (§5) |
| 5 | 연쇄 | `README.md` 인덱스가 갱신된 판정을 반영한다 | ✅ | 09-17 행 조건부 → 수용 가능, 09-25 행 추가 |
| 6 | 연쇄 | 코드·설정은 건드리지 않는다 | ✅ | diff 가 `docs/verification/` 안에서만 발생 |

## 3. 변경 사항
- `docs/verification/regression-list.md` — 신설. R-001~R-012
- `docs/verification/open-issues.md` — 신설. O-001~O-008 (OPEN 7 · CLOSED 1)
- `docs/verification/records/2026-09-17_account-thumbnail.md` — 🙋 4건을 오늘 근거로 ✅ 처리, 판정 조건부 → 수용 가능, §9 두 항목을 O-007·O-008 로 이관
- `docs/verification/README.md` — 09-17 행 판정 갱신, 09-25 행 추가
- 코드·DB·설정 변경: 없음

## 4. 영향 범위 분석
- 문서만 변경. `backend/`·`frontend/`·`docker-compose.yml` 무변경.
- CI(`deploy.yml`)는 `paths-ignore` 로 `**.md` 를 제외하므로 **이 push 는 배포를 일으키지 않는다.** (quiz·account 에서 같은 성질을 09-22 에 확인한 바 있음)

## 5. 실행한 검증
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 테스트 메서드명 대조 | 두 테스트 클래스에서 메서드명 추출 | `MonitoringControllerLogsTest` 5건·`HealthCheckServiceHealthTest` 7건 — regression-list 인용과 전부 일치 | ✅ |
| 기록 전수 확인 | `records/` 3건을 처음부터 끝까지 읽고 🙋·⬜·§9 추출 | 누락 없이 O-001~O-008 로 등재 | ✅ |
| O-001 운영 서빙 | `GET https://kyuhyeong.com/thumbnails/account.png` (브라우저 UA) | **200**, 134,053 bytes | ✅ |
| O-001 운영 DB 반영 | `GET https://kyuhyeong.com/api/projects` | account 행 `thumbnailUrl=/thumbnails/account.png` — 09-17 §6 의 UPDATE 가 이미 반영됨 | ✅ |
| O-001 화면 | 브라우저로 `https://kyuhyeong.com` 프로젝트 탭 | Account 카드에 세 화면(홈·영수증·영수증 확인) 나란히, 상하 잘림 자연스러움 | ✅ |
| itsm 숨김 유지 | 같은 `GET /api/projects` | id 1(itsm) 없음 — `visible=FALSE` 유지 (R-012) | ✅ |
| 카드 판정 | 같은 화면 | Song Quiz·Account 모두 **UP / running** | ✅ |
| 전체 회귀 | `backend/` `./gradlew test --no-daemon` | **26 tests, 0 failures, 0 errors** (문서만 바뀜으니 09-20 기준 26과 같아야 한다 — 일치) | ✅ |

## 6. 수동 확인 시나리오
- 해당 없음 — 문서 변경이고, 확인이 필요한 항목은 이번에 직접 확인했다(§5).

## 7. checklist 점검
- 점검함: 기존 기록 전수 확인, 인용한 식별자(테스트 메서드명)의 실제 존재 여부, 남은 🙋 의 현재 상태 재확인, CI 배포 유발 여부
- 해당 없음: DB·권한·입력 검증·동시성

## 8. 발견된 문제와 조치
- **문제**: 09-17 기록이 8일 동안 "조건부" 로 남아 있었지만, 실제로는 운영에 이미 반영돼 있었다(운영 DB UPDATE·썸네일 서빙·화면 모두 정상).
  🙋 를 등록해 두는 곳이 없어서 아무도 되돌아가 닫지 않은 것이다 — `open-issues.md` 가 없던 것의 직접적인 결과.
- **조치**: 두 파일을 만들고, 등록과 동시에 O-001 을 확인해 닫았다. 앞으로 🙋 는 등록 시점에 확인 방법이 함께 남는다.
- 추가한 테스트: 없음(문서 작업). 대신 자동 테스트가 없는 노출 가드를 O-005 로 등록했다.

## 9. 미검증 영역과 남은 위험
- `domain-map.md` 는 만들지 않았다 — 전역 §6 이 요구하는 파일은 records·README·open-issues·regression-list 이고, dashboard 는 도메인이 Project 카드·Monitoring 둘뿐이라 지도를 따로 둘 이득이 적다. 필요해지면 그때 추가한다.
- O-002~O-008 은 열린 채로 남는다 — 각 행의 "확인 방법" 참조.

## 10. Regression 등록
- 이 작업 자체는 로직 변경이 없어 신규 등록 없음.
- 기존 재현 테스트 12건을 `regression-list.md` R-001~R-012 로 **소급 등록**했다.
