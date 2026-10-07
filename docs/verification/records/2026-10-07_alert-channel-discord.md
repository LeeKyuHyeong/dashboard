# 상태 전이 알림 채널 (Discord webhook) — 준비 문서
- 일자: 2026-10-07
- 유형: 신규
- 우선순위: P1 (추정 — 운영 알림. 방문자 동선·데이터 저장과 무관하므로 P0 아님)
- 판정: **준비 단계 (미착수)** — 아래 §1 결정 항목에 답을 받은 뒤 구현한다. README 인덱스에는 완료 후 추가.

관련: `TODO.md` 운영자 관점 "알림 채널 1개", `open-issues.md` O-004.

## 1. 요청과 목적
- 사용자가 원한 것: `TransitionService` 가 뽑아낸 전이(UP→DOWN 등)가 로그에만 남는 상태를 끝내고, 알림 채널 1개로 내보낸다.
- 개발자 확인 결과(결정 사항): **대기 중** — §1.1
- 진행 중 둔 가정: §1.2

### 1.1 진행 전 확인 요청

| 구분 | # | 항목 | 추정 / 기본값 |
|---|---|---|---|
| 필수 | 1 | 채널은 **Discord webhook** 으로 확정하는가. UptimeRobot 이 이미 Discord 를 쓰므로 같은 서버(별도 채널 또는 같은 채널)에 모으는 것을 전제했다 | — |
| 필수 | 2 | webhook URL 전달 경로: GitHub Secret `DISCORD_WEBHOOK_URL` → `deploy.yml` 에서 `export` → `docker-compose.yml` 환경변수 `MONITORING_ALERT_WEBHOOK_URL`. `DB_PASSWORD` 와 같은 경로다. 이 방식이면 `D:\server-infra.md` 배포 매트릭스 갱신은 본인이 한다 | — |
| 선택 | 3 | 기동 알림: 첫 사이클(기준선 적재) 때 "기동 · 현재 이상 n건" 메시지 1건을 보낼지. 첫 사이클 무음 때문에 **부팅 시점에 이미 죽어 있던 컨테이너는 전이 알림이 영영 안 온다** — 기동 알림이 그 구멍을 메운다. main push 마다 1건씩 온다 | 기본: 보낸다 |
| 선택 | 4 | 전송 위치: 판정 스레드에서 인라인 + 유한 타임아웃 5초 (`markChecked()` 뒤, `broadcast()` 앞) | 기본: 인라인 |
| 선택 | 5 | 전송 실패 시 재시도 | 기본: 없음. 다음 전이가 어차피 다시 온다. 실패는 로그 1줄 |
| 선택 | 6 | 운영 실전 확인 방법: 배포 후 `docker pause itsm-batch` → DOWN 알림 → `docker unpause itsm-batch` → UP 알림. itsm 은 private 학습용이라 영향 없음. **운영 컨테이너를 건드리는 절차라 허가 필요** | 기본: 이 절차 |
| 선택 | 7 | flapping 억제(연속 N사이클 조건) | 기본: v1 에는 없음. 논리 이름 감시 + starting=UP 덕에 배포로는 전이가 안 나므로 flapping 원인이 현재 없음 |
| 선택 | 8 | 우선순위 | 기본: P1 |

### 1.2 가정
- Discord webhook 은 `POST {url}` 에 JSON `{"content": "..."}` 로 보내면 204 를 돌려준다. 본문 2,000자 상한. 분당 30회 제한은 사이클당 최대 1건이라 무관.
- Spring Boot 3.2.5 의 `RestClient`(starter-web 포함) 를 쓴다. 의존성 추가 없음.
- 로컬 dev 프로필에는 URL 이 없다 → 알림 비활성, 전이는 지금처럼 로그.

## 2. Acceptance Criteria (초안 — 합의 전)

| # | 구분 | 조건 | 상태 | 근거(예정) |
|---|---|---|---|---|
| 1 | 정상 | 감시 대상이 UP→DOWN 또는 →MISSING 으로 바뀌면 다음 사이클 안에 Discord 에 🔴 메시지 1건이 온다. 컨테이너 이름·이전→현재 상태·시각(KST)이 들어 있다 | ⬜ | `DiscordAlertNotifierTest` + 🙋 §6-2 |
| 2 | 정상 | 복구(→UP)되면 🟢 메시지가 온다 | ⬜ | 〃 + 🙋 §6-3 |
| 3 | 정상 | unexpected 등장/사라짐, 감시 대상 추가/제외도 메시지(🟡)로 온다 | ⬜ | `DiscordAlertNotifierTest` |
| 4 | 정상 | 한 사이클에 전이가 여러 건이면 **메시지 1건**으로 묶인다 | ⬜ | `DiscordAlertNotifierTest` |
| 5 | 정상 | (#3 채택 시) 기동 후 첫 판정 사이클에 "기동 · 현재 이상 n건 · unexpected m건" 메시지 1건 | ⬜ | `MonitoringSchedulerTest` + 🙋 §6-1 |
| 6 | 예외 | webhook URL 이 비어 있으면 HTTP 를 호출하지 않고, 기동 로그에 "알림 채널 미설정" 이 남으며, 전이는 지금처럼 WARN 로그에 남는다 | ⬜ | `DiscordAlertNotifierTest`, `contextLoads`(dev) |
| 7 | 예외 | 전송 실패(타임아웃·4xx·5xx·예외)해도 판정 루프·`/health/self`·SSE 는 영향이 없고, 실패 사유(HTTP 상태 또는 예외명)가 WARN 로그에 남는다 | ⬜ | `DiscordAlertNotifierTest`, `MonitoringSchedulerTest` |
| 8 | 경계 | 전송은 연결·읽기 타임아웃 5초 안에 끝난다 (무한 대기 없음) | ⬜ | `DiscordAlertNotifierTest`(타임아웃 설정 단언) |
| 9 | 경계 | 첫 사이클(기준선 적재)·판정 불가 사이클은 **전이 알림을 내지 않는다** — 기존 규칙 유지 | ⬜ | 기존 `TransitionServiceTest` 8건 그대로 + `MonitoringSchedulerTest` |
| 10 | 경계 | 본문이 2,000자를 넘으면 잘라서 보낸다 (Discord 가 400 으로 통째로 거부하지 않게) | ⬜ | `DiscordAlertNotifierTest` |
| 11 | 노출 | webhook URL 은 코드·yml·문서·로그 어디에도 적히지 않는다. 환경변수 **이름**만 적는다. 실패 로그에도 URL 을 찍지 않는다 | ⬜ | 코드 리뷰 + grep |
| 12 | 노출 | 메시지에 URL·비밀값이 없다. 컨테이너 이름은 운영자 전용 채널이므로 허용 | ⬜ | `DiscordAlertNotifierTest` |

## 3. 변경 사항 (계획)

| 파일 | 변경 |
|---|---|
| `backend/.../monitoring/config/MonitoringProperties.java` | `Alert alert` 중첩 추가: `webhookUrl`(기본 ""), `timeoutSeconds`(기본 5), `notifyOnStartup`(기본 true). `@ConfigurationProperties` 경유라 환경변수 `MONITORING_ALERT_WEBHOOK_URL` 이 relaxed binding 으로 `monitoring.alert.webhookUrl` 에 붙는다 (CLAUDE.md "설정 키 바인딩 규칙" 준수 — `${...}` 플레이스홀더 사용 안 함) |
| `backend/.../monitoring/service/DiscordAlertNotifier.java` (신규) | `notifyTransitions(List<String>)`, `notifyStartup(MonitoringInventory)`. 메시지 조립(심각도 이모지·KST 시각·줄 묶기·2,000자 절단) + `RestClient` POST. URL 비면 no-op. 모든 예외를 잡아 WARN 로그 1줄 |
| `backend/.../monitoring/service/MonitoringScheduler.java` | `logTransitions()` 가 전이 목록을 notifier 에도 넘긴다. 순서: `markChecked()` → 전이 로그+알림 → `broadcast()`. 첫 사이클은 `TransitionService` 가 빈 목록을 주므로 기동 알림은 "기준선이 방금 적재됐는가" 를 따로 받아 보낸다 (`TransitionService.evaluate` 반환형은 유지, 기준선 적재 여부를 알 수 있는 작은 변경 필요 — 구현 시 결정) |
| `backend/src/main/resources/application.yml` | `monitoring.alert` 키와 주석(환경변수 이름, 비면 비활성). **URL 값은 적지 않는다** |
| `docker-compose.yml` | `MONITORING_ALERT_WEBHOOK_URL=${DISCORD_WEBHOOK_URL:-}` 1줄 |
| `.github/workflows/deploy.yml` | `export DISCORD_WEBHOOK_URL='${{ secrets.DISCORD_WEBHOOK_URL }}'` 1줄 (`DB_PASSWORD` 옆) |
| `CLAUDE.md` | "상태 전이 + 첫 사이클 무음" 절에 알림 채널·환경변수 이름·비활성 조건 3줄 |
| `TODO.md` | 운영자 관점 "알림 채널 1개" 체크 |
| `docs/verification/open-issues.md` | O-004 → CLOSED (완료 후) |
| 테스트 (신규) | `DiscordAlertNotifierTest` (포맷·무음·실패 격리·타임아웃·절단), `MonitoringSchedulerTest` (호출 순서·실패 격리, Mockito) |

- DB·설정 변경: DB 없음. 설정 키 신규 `monitoring.alert.webhookUrl` · `monitoring.alert.timeoutSeconds` · `monitoring.alert.notifyOnStartup`. 환경변수 `MONITORING_ALERT_WEBHOOK_URL`, GitHub Secret `DISCORD_WEBHOOK_URL`.

### 메시지 형식 (안)

```
🔴 [dashboard] 2026-10-07 14:03 KST — 전이 2건
• quiz-app UP -> DOWN
• unexpected 등장: stranger-app
```

- 심각도: 한 건이라도 `-> DOWN` / `-> MISSING` / `감시 대상에서 제외됨` 이면 🔴, 아니면 `unexpected` 가 있으면 🟡, 전부 `-> UP` 이면 🟢.
- 기동: `🔵 [dashboard] 기동 · 기준선 적재 · 현재 이상 1건: itsm-batch(DOWN) · unexpected 0건`

## 4. 영향 범위 분석
- `TransitionService.evaluate` 호출처: `MonitoringScheduler.logTransitions` 1곳만 (grep 확인). 반환형 유지.
- `MonitoringScheduler.collectAndBroadcast` 의 예외 처리: notifier 가 예외를 삼키므로 `markFailed` 로 떨어지지 않는다. 떨어지면 `/health/self` 가 500 이 되어 UptimeRobot 이 "판정 불가"로 읽는 오류가 생기므로 **반드시 격리** (AC 7).
- 영향 받는 화면/기능: 없음(프론트 변경 없음). `/health/self`·SSE 는 순서상 알림 앞/뒤에 있어 동작 불변.
- `DashboardApplicationTests.contextLoads`(dev): 새 프로퍼티에 기본값이 있어야 URL 없이 컨텍스트가 뜬다 (AC 6).
- 호출 비용: 사이클당 HTTP 1회 이하, 전이가 없는 사이클은 0회.

## 5. 실행한 검증 (계획 — 구현 후 채움)
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 빌드 | `backend/` `./gradlew build` | | ⬜ |
| Unit | `./gradlew test --tests "*AlertNotifier*" --tests "*MonitoringScheduler*"` | | ⬜ |
| 전체 회귀 | `./gradlew test` (현재 26건 + 신규) | | ⬜ |
| 프론트 | 변경 없음 | | 해당 없음 |
| 사용자 시나리오 | 아래 §6 (운영 반영 후) | | 🙋 |

## 6. 수동 확인 시나리오 (운영 반영 후, 🙋)
1. [전제] GitHub Secret `DISCORD_WEBHOOK_URL` 등록 → main push → 배포 완료.
   [기대] (#3 채택 시) 기동 약 60~90초 뒤 Discord 에 🔵 기동 메시지 1건. 기동 로그에 `알림 채널: Discord 활성` 류 1줄.
2. [행동] 서버에서 `docker pause itsm-batch`
   [기대] 최대 120초 안에 🔴 `itsm-batch UP -> DOWN` 메시지 1건. 카드는 없으므로 화면 변화 없음. `/health/self` 는 200 유지.
3. [행동] `docker unpause itsm-batch`
   [기대] 최대 120초 안에 🟢 `itsm-batch DOWN -> UP` 메시지 1건.
4. [행동] 로컬 dev(`--spring.profiles.active=dev`) 기동
   [기대] 기동 로그에 "알림 채널 미설정" 1줄, HTTP 호출 없음.
- 결과: ☐ 통과 ☐ 실패

## 7. checklist 점검
- 구현 후 `~/.claude/verification/checklist.md` 로 점검.

## 8. 발견된 문제와 조치
- (구현 후)

## 9. 미검증 영역과 남은 위험
- Discord 실제 전송은 로컬에서 재현 불가(비밀값 없음) → 운영 반영 후 🙋 §6.
- 2,000자 절단 경계값은 단위 테스트로만 확인(실제 Discord 의 거부 동작은 호출하지 않음).

## 10. Regression 등록 (예정)
- R-015 전송 실패가 판정 루프·`/health/self` 를 깨지 않는다 — `MonitoringSchedulerTest`
- R-016 URL 미설정 시 HTTP 호출 없음 — `DiscordAlertNotifierTest`
- R-017 첫 사이클·판정 불가 사이클은 알림 없음 — 기존 `TransitionServiceTest` + `MonitoringSchedulerTest`
