# 로그 조회 대상 검증 + HEALTHCHECK 결과를 판정에 반영
- 일자: 2026-09-20
- 유형: 버그 (보안 1 + 판정 누락 1)
- 우선순위: P1 (판정 규칙은 P0 시나리오 2 — 단위 테스트가 계약)
- 판정: 조건부 — 자동 검증 ✅, 실제 docker 출력·운영 화면 확인이 🙋

## 1. 요청과 목적
- 사용자가 원한 것: Redis·AWS 작업 전에 운영 중인 사이트의 구멍부터 막는다 (quiz 인증 방어와 같은 묶음).
- 발견한 결함 (2026-09-19)
  1. `GET /api/monitoring/logs/{containerName}` 이 경로의 이름을 그대로 `docker logs` 에 넘겼다. 인자 목록 실행이라 셸 주입은 안 되지만 ① 호스트의 **아무 컨테이너**(DB·fail2ban 포함) 로그를 읽을 수 있고 ② `-f` 같은 값은 docker 옵션으로 해석돼 10초 타임아웃까지 워커 스레드를 붙잡는다. 운영은 nginx 가 경로를 통째로 막아(`return 404`) 지금 외부에서 부를 수는 없다 — **그 한 줄이 빠지면 열리는** 구멍.
  2. 판정이 `State.Status` 만 읽어 `running` 이면 UP 이었다. HEALTHCHECK 가 unhealthy 여도(DB 접속 불가, 앱 무응답) 화면은 UP 이고 전이도 남지 않았다. 감시 대상 11개 중 HEALTHCHECK 가 있는 것: quiz-app(blue/green)·quiz-db·account-db·itsm-db·dashboard-db·itsm-frontend.
- 개발자 확인 결과(결정 사항, 2026-09-20)
  - 로그 경로는 제거하지 않고 허용 목록으로 검증한다.
  - unhealthy 는 새 상태를 만들지 않고 **DOWN 으로 합친다.**
  - `starting` 은 UP 으로 본다(에이전트 제안, 이견 없음).
- 진행 중 둔 가정: `docker inspect --format` 의 `{{if .State.Health}}…{{else}}none{{end}}` 가 운영 docker 버전에서 기대대로 출력된다 → §6-1 로 확인.

## 2. Acceptance Criteria
| # | 구분 | 조건 | 상태 | 근거 |
|---|---|---|---|---|
| C1-1 | 정상 | 감시 대상(`expected` + 그룹 멤버)의 실제 이름은 로그를 돌려준다 | ✅ | `MonitoringControllerLogsTest#감시_대상_*`, `#그룹_멤버의_*` |
| C1-2 | 예외 | 감시 대상이 아닌 이름(`house-db`·임의 문자열·`-f`·`--since=…`·빈 값)은 404, **docker 를 실행하지 않는다** | ✅ | `#감시_대상이_아닌_*` (수정 전: 전부 실행됨) |
| C1-3 | 경계 | 논리 이름 `quiz-app` 은 실제 컨테이너가 아니므로 404 | ✅ | `#논리_이름은_*` |
| C1-4 | 노출 | 거부 응답에 컨테이너 이름·목록이 없다 | ✅ | `#감시_대상이_아닌_*` |
| C1-5 | 정상 | tail 1~1000 제한·타임아웃은 그대로 | ✅ | `#tail_범위_*` |
| C2-1 | 정상 | running + healthy → UP | ✅ | `HealthCheckServiceHealthTest#running_이고_healthy_*` |
| C2-2 | 예외 | running + unhealthy → DOWN, inventory 이상, 카드 Docker 칸에 `unhealthy` | ✅ | `#running_이어도_unhealthy_*` (수정 전 UP) |
| C2-3 | 경계 | HEALTHCHECK 없는 컨테이너는 이전처럼 running 이면 UP. 템플릿에 nil 가드가 있다(없으면 행이 빠져 MISSING 오판) | ✅ | `#HEALTHCHECK_가_없는_*`, `#inspect_템플릿은_*` |
| C2-4 | 경계 | `starting` 은 UP — 드레인 중 새 색이 starting 이어도 이상 없음 | ✅ | `#starting_은_*` |
| C2-5 | 경계 | 그룹 해석(running 우선 → 최근 기동)은 그대로, 대기 색 Exited 는 정상 | ✅ | `#대기_색이_*` + 기존 `HealthCheckServiceGroupTest` 5건 |
| C2-6 | 예외 | 멈춘 컨테이너는 health 와 무관하게 DOWN, Docker 칸은 `exited` 그대로 | ✅ | `#멈춘_컨테이너는_*` |
| C2-7 | 예외 | docker 조회 실패는 UNKNOWN (DOWN 과 섞이지 않음) | ✅ | 기존 `#docker_조회_실패는_*` |
| C2-8 | 연쇄 | `/health/self` 는 감시 대상의 unhealthy 를 반영하지 않는다(변경 없음) | ✅ | 코드 변경 없음 — 판정 루프 생존만 본다 |
| C2-9 | 정상 | 실제 docker 가 HEALTHCHECK 유·무 컨테이너 모두에서 4칸 행을 낸다 | 🙋 | §6-1 |
| C2-10 | 정상 | 운영 화면에서 카드가 전부 UP 으로 보인다(행 누락으로 MISSING 이 되지 않는다) | 🙋 | §6-2 |

## 3. 변경 사항
- `monitoring/controller/MonitoringController#getLogs` — `props.watchedContainerNames()` 에 없는 이름은 404, docker 호출 전에 거른다
- `monitoring/service/HealthCheckService` — inspect 템플릿에 4번째 칸(health, nil 가드), `ContainerState.health` + `up()`(running 이고 unhealthy 가 아님). `compare`·`toStatus` 가 `up()` 을 쓴다. unhealthy 일 때 `ServiceStatus.dockerStatus = "unhealthy"`
- 프론트 변경 없음 — 배지는 UP/UNKNOWN 이 아니면 down 스타일, Docker 칸은 `running` 이 아니면 빨간 글씨로 값을 그대로 보여준다
- DB·설정 변경: 없음 (`application.yml` 키 변경 없음)

## 4. 영향 범위 분석
- `ContainerState` 생성처 1곳(snapshot). `dockerStatus` 비교처: `resolve`(그룹 대표 선택 — running 기준 유지, 의도), `compare` 의 unexpected 필터(running 기준 유지 — 낯선 컨테이너는 health 와 무관하게 경고)
- `watchedContainerNames()` 는 unexpected 판정이 쓰던 기존 메서드 — 변경 없이 재사용
- 로그 모달(`LogModal.jsx`)은 `ServiceStatus.containerName`(해석된 실제 이름)을 보낸다 → 허용 목록에 포함. 운영은 nginx 404 라 동작 변화 없음, 로컬 dev 는 docker 가 없어 원래 502
- **기존 테스트 수정 1개 파일(단언 변경 없음)**: `HealthCheckServiceGroupTest` 의 가짜 docker 출력에 4번째 칸 `none` 추가 — inspect 출력 형식이 바뀐 데 따른 픽스처 변경
- 전이 로그: 배포 시점에 unhealthy 인 컨테이너가 있으면 첫 사이클 무음 규칙으로 기준선에만 들어간다(알림 폭주 없음)

## 5. 실행한 검증
| 계층 | 명령/방법 | 결과 | 상태 |
|---|---|---|---|
| 재현(수정 전) | 새 테스트 2개 클래스 | 12건 중 9건 실패 (임의 이름으로 docker 실행, unhealthy 가 UP, 4칸 출력 미지원) | ✅ |
| 빌드 + 전체 회귀 | `backend/` `./gradlew test` | 26 passed (14 + 신규 12), 0 failed | ✅ |
| 프론트 | 변경 없음 | — | 해당 없음 |
| 실제 docker 출력 | §6-1 | — | 🙋 |
| 배포 후 Smoke | §6-2 | — | 🙋 |

## 6. 수동 확인 시나리오
### 6-1. 배포 전 — 서버에서 템플릿 출력 확인 (읽기 전용)
```bash
docker inspect --format '{{.Name}}	{{.State.Status}}	{{.State.StartedAt}}	{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' quiz-db account-api itsm-fail2ban
```
(칸 사이는 탭 문자) [기대] 3행, 각 4칸 — `/quiz-db running <시각> healthy`, `/account-api running <시각> none`, `/itsm-fail2ban running <시각> none 또는 healthy`(`crazymax/fail2ban` 이미지가 자체 HEALTHCHECK 를 갖고 있을 수 있다 — compose 에는 없지만 이미지에 선언돼 있으면 값이 나온다. 어느 쪽이든 4칸이면 정상). 에러(`template: … nil pointer`)가 나오면 배포하지 않는다.

### 6-2. 배포 후
1. `https://kyuhyeong.com` 프로젝트 탭. [기대] Song Quiz·Account 카드가 **UP / running** (MISSING·DOWN 이 아님).
2. 서버: `docker logs --since 5m dashboard-app 2>&1 | grep -E "전이|이상|MISSING"` [기대] "전이 기준선 적재 … 현재 이상 0건". 0 이 아니면 어떤 컨테이너가 unhealthy 인지 `docker ps --format '{{.Names}} {{.Status}}'` 로 확인 — **실제로 unhealthy 인 컨테이너를 이번에 처음 보게 되는 것일 수 있다**(그 경우 판정이 맞다).
3. 에이전트: 외부에서 `/api/monitoring/health/self` 200, `/api/monitoring/logs/x` 404(nginx) 확인.

## 7. checklist 점검
- 점검함: 입력 검증 위치(외부 프로세스 호출 전) / 거부 응답의 정보 노출 / 출력 형식 변경의 파싱 호환(칸 수가 다르면 행을 버린다 → 템플릿·파서·테스트 픽스처를 함께 변경) / 배포 직후 전이 폭주 여부(첫 사이클 무음)
- 해당 없음: DB·프로파일, 인증(앱에 없음), 프론트 코드

## 8. 발견된 문제와 조치
- 없음 (구현 중 추가 결함 없음)

## 9. 미검증 영역과 남은 위험
- `starting` 을 UP 으로 보므로 start_period 안에서 계속 실패 중인 컨테이너는 그동안 UP 으로 보인다(quiz 최대 60초 + retries)
- HEALTHCHECK 가 없는 5개(account-api·itsm-api·itsm-batch·dashboard-app·itsm-fail2ban)는 여전히 "떠 있으면 UP" — 각 앱 리포에서 HEALTHCHECK 를 추가해야 닫힌다
- 알림 채널 없음(전이는 로그에만) — 이번 범위 밖, 외부 감시는 UptimeRobot + Discord

## 10. Regression 등록
- `HealthCheckServiceHealthTest`, `MonitoringControllerLogsTest` (이 저장소에는 regression-list 파일이 아직 없다 — 테스트 클래스 주석에 발견 경위를 남김)
