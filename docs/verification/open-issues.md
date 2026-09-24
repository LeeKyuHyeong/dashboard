# Open Issues

미검증(⬜)·직접 확인 필요(🙋)·보류 항목. 상태: OPEN / IN-PROGRESS / CLOSED. 닫을 때 해결 기록 링크를 남긴다.
초기 항목은 `records/` 3건의 §9(미검증 영역)·🙋 AC 에서 옮겼다(2026-09-25 정비).

| ID | 상태 | 도메인 | 내용 | 미검증/보류 이유 | 확인 방법 | 등록 | 해결 기록 |
|---|---|---|---|---|---|---|---|
| O-001 | CLOSED | UI/Content | Account 카드 썸네일(3화면 합성)의 운영 DB UPDATE·서빙·화면 확인 | 운영 DB 는 시드를 읽지 않아(`spring.sql.init.mode: never`) 본인이 UPDATE 해야 했고, 카드 크롭 정도는 실제 화면에서만 판단 가능 | records/2026-09-17_account-thumbnail §6 | 2026-09-17 | **2026-09-25 닫힘** — `GET /thumbnails/account.png` 200 (134,053 bytes), `GET /api/projects` 의 account 행 `thumbnailUrl=/thumbnails/account.png`(운영 UPDATE 반영 확인), 브라우저 화면에서 세 화면 나란히·잘림 자연스러움. records/2026-09-25_verification-docs-setup §5 |
| O-002 | OPEN | Monitoring | HEALTHCHECK 가 없는 컨테이너(`itsm-api`·`itsm-batch`)는 여전히 "떠 있으면 UP" — 프로세스가 살아만 있고 응답을 못 해도 UP 으로 보인다 | HEALTHCHECK 는 각 앱 리포의 `docker-compose.yml`/`Dockerfile` 에서 선언해야 한다. dashboard 리포에서 닫을 수 없음 | itsm 리포에 HEALTHCHECK 추가 → 이 리포 변경 없이 판정에 자동 반영(R-006·R-007 이 이미 커버) | 2026-09-20 | |
| O-003 | OPEN | Monitoring | `starting` 을 UP 으로 보므로, start_period 안에서 계속 실패 중인 컨테이너는 그동안 UP 으로 보인다(quiz 최대 60초 + retries) | 의도한 절충 — `starting` 을 DOWN 으로 보면 배포 때마다 이상 전이가 찍힌다(개발자 결정 2026-09-20) | start_period 를 넘겨 unhealthy 로 확정되면 DOWN 이 된다. 이 구간을 좁히려면 컨테이너별 start_period 조정 | 2026-09-20 | |
| O-004 | OPEN | Ops | 상태 전이에 알림 채널이 없다 — 전이는 애플리케이션 로그에만 남는다 | 이번 범위 밖(2026-09-20). 외부 감시는 UptimeRobot + Discord 로 따로 둠 | 전이 발생 시 Discord webhook 등으로 내보낼지 결정 | 2026-09-20 | |
| O-005 | OPEN | Test | `ProjectController` 의 `visible` 필터(숨긴 카드를 목록·상세 양쪽에서 차단)에 자동 테스트가 없다 | 09-17 작업에서 코드 라인(`ProjectController.java:21`·`:30`)과 운영 조회로만 확인했다 | 목록·상세 각각에 대해 `visible=false` 행이 안 나오는 테스트 추가 → R-012 를 수동에서 자동으로 승격 | 2026-09-25 | |
| O-006 | OPEN | Content | itsm 카드를 다시 공개할 때 운영 DB UPDATE 와 시드 두 벌(`data-h2.sql`·`data-mariadb.sql`)을 **함께** 되돌려야 한다 | itsm 앱에 시연 계정 로그인 실패 등 정리할 오류가 남아 공개 보류(본인 결정 2026-09-17) | itsm 정리 완료 후 `visible=TRUE`·`github_url` 복원을 운영·시드 동시 적용 | 2026-09-17 | |
| O-007 | OPEN | UI | 카드 3열 그리드에서 썸네일 `object-fit:cover` 크롭 정도는 실제 화면에서만 확인 가능 | 현재 공개 카드가 2개(itsm 숨김)라 3열이 만들어지지 않는다 — O-006 이 풀려야 발생 | itsm 공개 후 넓은 화면에서 3열 배치 확인 | 2026-09-17 | |
| O-008 | OPEN | Build | 프론트+백엔드 결합 빌드(루트 `Dockerfile` 멀티스테이지)를 로컬에서 돌린 적이 없다 — CI(`deploy.yml`) 성공에만 의존한다 | 로컬에 결합 빌드를 돌릴 이유가 없었고, 배포는 CI 가 한다 | 결합 빌드가 깨지면 CI 가 먼저 잡는다. 로컬 확인이 필요하면 루트에서 `docker build .` | 2026-09-17 | |
