# Regression 목록

버그를 고치면 재현 테스트를 남기고 여기에 1행 추가한다.
자동(테스트 클래스) 항목은 `backend/` 에서 `./gradlew test` 로 전부 돈다. 수동 항목은 배포 후 확인한다.
초기 항목은 `records/2026-09-20_logs-allowlist-and-healthcheck.md` 에서 옮겼다
(그 기록 §10 이 "이 저장소에는 regression-list 파일이 아직 없다" 라고 남긴 것을 2026-09-25 에 해소).

| ID | 도메인 | 시나리오 | 검증 방법 | 출처 | 등록일 |
|---|---|---|---|---|---|
| R-001 | Monitoring/Security | 감시 목록에 없는 컨테이너 이름은 **docker 를 실행하지 않고** 404 (`house-db`·임의 문자열·`-f`·`--since=…`·빈 값) | `MonitoringControllerLogsTest#감시_대상이_아닌_이름은_docker_를_실행하지_않고_404` | records/2026-09-20 C1-2 | 2026-09-20 |
| R-002 | Monitoring/Security | 논리 이름(`quiz-app`)은 실제 컨테이너가 아니므로 404 | `MonitoringControllerLogsTest#논리_이름은_실제_컨테이너가_아니므로_거부한다` | records/2026-09-20 C1-3 | 2026-09-20 |
| R-003 | Monitoring/Security | 거부 응답에 컨테이너 이름·감시 목록이 실리지 않는다 | `MonitoringControllerLogsTest#감시_대상이_아닌_이름은_docker_를_실행하지_않고_404` (본문 단언) | records/2026-09-20 C1-4 | 2026-09-20 |
| R-004 | Monitoring | tail 1~1000 범위 제한과 타임아웃이 유지된다 | `MonitoringControllerLogsTest#tail_범위_제한은_그대로다` | records/2026-09-20 C1-5 | 2026-09-20 |
| R-005 | Monitoring | 감시 대상·그룹 멤버의 실제 이름은 로그를 돌려준다 (허용 목록이 정상 경로를 막지 않는다) | `MonitoringControllerLogsTest#감시_대상_컨테이너는_로그를_돌려준다`, `#그룹_멤버의_실제_이름은_받는다_화면이_보내는_이름이다` | records/2026-09-20 C1-1 | 2026-09-20 |
| R-006 | Monitoring | running 이어도 unhealthy 면 DOWN 이고 카드 Docker 칸에 `unhealthy` 가 보인다 (수정 전: UP) | `HealthCheckServiceHealthTest#running_이어도_unhealthy_면_DOWN_이고_이유가_카드에_보인다` | records/2026-09-20 C2-2 | 2026-09-20 |
| R-007 | Monitoring | `docker inspect` 템플릿의 nil 가드 — HEALTHCHECK 가 없는 컨테이너에서도 행이 나온다 (칸 수가 어긋나면 행이 버려져 MISSING 오판) | `HealthCheckServiceHealthTest#inspect_템플릿은_HEALTHCHECK_가_없는_컨테이너에서도_행을_낸다` | records/2026-09-20 C2-3 | 2026-09-20 |
| R-008 | Monitoring | HEALTHCHECK 가 없는 컨테이너는 이전처럼 running 이면 UP (기존 동작 유지) | `HealthCheckServiceHealthTest#HEALTHCHECK_가_없는_컨테이너는_이전처럼_running_이면_UP` | records/2026-09-20 C2-3 | 2026-09-20 |
| R-009 | Monitoring | `starting` 은 UP — 배포 때마다 이상 전이가 찍히지 않는다 | `HealthCheckServiceHealthTest#starting_은_UP_으로_본다_배포_때마다_이상_전이가_찍히지_않게` | records/2026-09-20 C2-4 | 2026-09-20 |
| R-010 | Monitoring | 그룹 해석에서 대기 색이 Exited 여도 활성 색이 healthy 면 UP | `HealthCheckServiceHealthTest#대기_색이_Exited_여도_활성_색이_healthy_면_UP` + `HealthCheckServiceGroupTest` 5건 | records/2026-09-20 C2-5 | 2026-09-20 |
| R-011 | Monitoring | 멈춘 컨테이너는 health 와 무관하게 DOWN, Docker 칸은 docker 상태 그대로 | `HealthCheckServiceHealthTest#멈춘_컨테이너는_health_와_무관하게_DOWN_이고_상태_칸은_docker_상태_그대로다` | records/2026-09-20 C2-6 | 2026-09-20 |
| R-012 | Content | 숨긴 카드(`visible=FALSE`)는 목록에도 상세에도 나오지 않는다 (itsm) | **수동** — `GET /api/projects` 에 id 1 없음. 자동 테스트 없음(O-005) | records/2026-09-17_itsm-card-hidden-seed AC5 | 2026-09-17 |
