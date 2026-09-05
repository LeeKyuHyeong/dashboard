# Claude Code 작업 지시 — dashboard 모니터링 1-3

> 이 파일 전체를 IntelliJ의 Claude Code 세션 첫 메시지로 붙여넣는다.
> 대상 저장소: `LeeKyuHyeong/dashboard`
> 작성일: 2026-09-05

---

## 0. 나에 대해

SI 4년차 Java 백엔드 개발자다. 실무는 eGovFrame/JSP/MyBatis/Oracle.
이 인프라를 AI 도움으로 구축했고, **스스로 설명할 수 있는 수준까지 이해하는 것이 목표**다.

그래서 다음을 지켜라.

- 명령어와 코드만 주지 말고 **왜 그렇게 하는지, 대안은 무엇이었는지** 설명할 것
- 내가 놓친 실패 케이스를 **먼저** 지적할 것
- 내 판단이 틀렸으면 동의부터 하지 말고 **직접** 말할 것
- 되돌릴 수 없는 작업은 **반드시 먼저 경고**할 것
- **설정에서 실행 상태를 추론하지 말 것.** 코드를 직접 읽고 근거를 인용할 것
- 아래 "확정된 판단"과 다른 얘기를 하려면 **왜 다른지 먼저 밝힐 것**

---

## 1. 시스템 개요

Cafe24 VPS 1대(4GB, CentOS, cgroup v1)에 Docker Compose + nginx로 토이 프로젝트 5개를 운영한다.
`dashboard`는 그중 **운영 도구** 역할이고, 다른 컨테이너들을 감시한다.

**컨테이너 11개**

| 종류 | 이름 |
|---|---|
| 자바 앱 5 | quiz-app, itsm-api, itsm-batch, dashboard-app, account-api |
| 프론트 1 | itsm-frontend (nginx) |
| DB 4 | quiz-db, itsm-db, dashboard-db, account-db |
| 보안 1 | itsm-fail2ban |

`house-app` / `house-db`는 2026-09-04에 down (account 통합 예정).

**도커 네트워크 (실측, 09-05)**

```
dashboard-app  →  dashboard_dashboard-net (172.28.0.3) + itsm_backend (172.26.0.5)
quiz-app       →  quiz_default
account-api    →  account_account-net
itsm-api       →  itsm_backend + itsm_frontend
```

전 컨테이너 포트가 호스트의 `127.0.0.1`에만 바인딩돼 있다(2026-07 보안 조치).

**배포**: GitHub Actions. `:latest`와 커밋 해시 태그를 함께 푸시하므로 롤백 가능.
서버에서 이미지만 갈아끼운다(`git pull` 없음). **즉 저장소의 파일을 서버가 읽지 않는다.**

---

## 2. 이번 작업 범위 — 1-3 (판정 로직)

한 배포로 묶어서 처리한다. 나누면 dashboard를 세 번 배포해야 하고, 이 프로젝트는
배포 때마다 crash-loop 전례가 있다.

| # | 항목 |
|---|---|
| 1 | **HTTP 폴링 전면 제거.** 판정은 컨테이너 상태 단일 |
| 2 | **기대 목록**(`expected` / `ignored`) + **양방향 비교** |
| 3 | **상태 전이 판정** + **첫 사이클 무음** |
| 4 | 모든 외부 호출에 **유한 타임아웃**, SSE 팬아웃 격리 |

**이번 범위가 아닌 것**: Discord 알림 전송(1-4), 메트릭 시계열/DB 테이블(2단계),
Actuator 도입, 인증 경로 분리.

---

## 3. 확정된 판단 — 근거와 함께

이미 조사와 실측을 거쳐 정해진 것들이다. 뒤집으려면 근거를 대라.

### 3-1. HTTP 헬스체크를 없앤다

현재 dashboard는 감시 대상을 **공개 도메인으로** 폴링한다. nginx 로그로 확인된 실물:

```
172.28.0.3 - "GET / HTTP/1.1" 200 9320 "Apache-HttpClient/5.3.1" host=game.kyuhyeong.com
```

문제가 세 겹이다.

1. **판정 범위 오염** — 이 요청은 nginx·TLS·라우팅·앱을 한꺼번에 통과한다.
   quiz가 죽어도, nginx가 죽어도, 인증서가 만료돼도 똑같이 실패한다.
   "무엇이 죽었나"를 알려주는 것이 dashboard의 존재 이유인데 정작 구분을 못 한다.
2. **UptimeRobot과 중복** — 2026-09-05에 외부 감시(UptimeRobot 5분 간격)를 붙였고
   quiz·account·dashboard를 같은 URL로 이미 보고 있다. 안에서 또 볼 이유가 없다.
3. **자가 트래픽** — 60초마다 홈페이지 전체 9KB를 받아 버린다. 하루 1,440회.
   quiz는 메모리 관찰 중인 대상이라 관찰 기준선까지 오염시킨다.

또한 HTTP로 판정 가능한 앱이 애초에 없다 —
itsm 403(Security가 가로챔) / dashboard 404(Actuator 없음) / account 네트워크 미연결.

**대안 검토와 기각 사유**

| 대안 | 기각 이유 |
|---|---|
| 도커 게이트웨이 IP 경유(`172.28.0.1:8082`) | 포트가 `127.0.0.1`에만 바인딩돼 있어 브리지 인터페이스로는 안 닿는다 |
| dashboard를 각 스택 네트워크에 추가 연결 | **그 스택을 내리면 dashboard가 기동하지 못한다.** 감시 도구가 감시 대상 때문에 못 뜨는 구조 |
| 공동 `monitoring` 네트워크 신설 | compose 5개 수정. 이번 범위를 넘고, 위 기동 의존 문제도 남는다 |

→ HTTP 판정은 **Actuator 도입 이후 내부 경로로 재도입**한다. 지금은 걷어낸다.

### 3-2. 기대 목록은 jar 안 yml + 양방향 비교

`monitoring.services`(화면 카드용, 앱 3개)와 **판정 대상 11개는 다른 목록**이다.
DB 카드를 화면에 그릴 이유가 없으므로 한 목록으로 합치면 계속 어색해진다.

```yaml
monitoring:
  services:   # 화면 카드용 (기존)
    - ...
  expected:   # 판정 대상 11개
    - quiz-app
    - quiz-db
    # ...
  ignored:    # 의도적 제외 + 사유 주석
    - house-app     # account 통합 예정, 09-04 down
    - house-db
```

**양방향 비교가 핵심이다.**

- 목록에 있는데 실행 중이 아님 → 이상
- **목록에 없는데 실행 중 → 경고**

후자가 없으면 새 앱을 올리고 목록 갱신을 잊었을 때 **감시에서 조용히 빠진다.**

**대안 기각**

- 호스트 파일 마운트 → dashboard 배포가 이미지만 갈아끼우므로 서버 파일을 갱신할 주체가 없다
- Docker label → 컨테이너가 사라지면 label도 사라진다. **감지해야 할 바로 그 순간에 근거가 증발**

### 3-3. `/api/monitoring/health/self`의 응답 코드 의미

**이 엔드포인트는 UptimeRobot이 5분마다 친다. 의미를 바꾸면 외부 감시가 오염된다.**

| 코드 | 의미 |
|---|---|
| 200 | 판정 루프가 최근에 돌았다 |
| 503 | **판정 루프가 멈췄다** (`lastCheckAgeSec` 임계 초과) |
| 500 | 판정 자체가 불가 (docker 소켓 실패 등) |

**감시 대상 컨테이너가 죽은 것은 여기에 반영하지 않는다.** 반영하면
컨테이너 하나가 죽을 때마다 UptimeRobot이 "사이트 다운"으로 판정하고,
호스트·nginx·dashboard 자체의 다운과 구분이 불가능해진다.

응답 본문에 컨테이너 이름·상태를 담지 않는다. 이 경로는 무인증 공개다.

### 3-4. 첫 사이클 무음

전이 판정은 이전 상태를 기억해야 성립하는데, 그 상태가 인메모리면
**재시작 때마다 baseline이 날아간다.** crash-loop이면 재시작마다 알림이 반복된다.

→ 부팅 후 **첫 사이클은 상태 적재만 하고 전이로 취급하지 않는다.**

DB 저장은 하지 않는다. 이 프로젝트는 마이그레이션 수단이 없다
(`ddl-auto: none` + `CREATE TABLE IF NOT EXISTS`라 기존 테이블 변경이 반영되지 않는다).
Flyway 도입은 2단계 결정 사항이다.

### 3-5. 타임아웃이 본질이다

`MonitoringScheduler`는 스레드 1개(`scheduling-1`)에서 돌고,
그 위에서 `SseEmitterService.broadcast()`가 `emitter.send()`를 순차 호출한다.

반쯤 끊긴 TCP는 `IOException`을 던지지 않고 **그냥 멈춘다.**
`new SseEmitter(Long.MAX_VALUE)`라 `onTimeout` 경로도 없다.
브라우저 탭 하나가 남아 있으면 판정 루프 전체가 정지할 수 있다.

**풀 크기를 늘리는 것으로는 부족하다** — 블로킹 emitter가 풀 크기만큼이면 동일하다.

| 대상 | 조치 |
|---|---|
| SSE 팬아웃 | 별도 executor + emitter 타임아웃 유한값(예: 5분) + 클라이언트 재연결 |
| docker 호출 | 연결·읽기 타임아웃 |

`markChecked()`를 `broadcast()` **앞**에 두는 순서는 유지한다 — SSE가 막혀도 판정은 끝난 것으로 본다.

---

## 4. 먼저 조사하고 보고하라 — 코드 작성 금지

**첫 응답에서는 코드를 쓰지 마라.** 아래를 읽고 보고만 해라.

읽을 것:

- `MonitoringScheduler`
- `HealthCheckService`
- `SseEmitterService`
- `MonitoringController`
- `application.yml` (특히 `monitoring:` 블록 전체)
- `build.gradle` / `pom.xml` 의존성
- `.github/workflows/` 배포 워크플로
- `docker-compose.yml`

보고할 것:

1. **docker 호출 방식** — 서브프로세스(`ProcessBuilder`)인가 `docker-java`인가.
   문서에는 두 가지가 엇갈리게 적혀 있다. 실제 코드로 확정하고,
   현재 타임아웃이 어디에 어떻게 걸려 있는지 인용해라
2. **상태 판정이 결정되는 지점** — HTTP 결과와 컨테이너 상태를 어떻게 합치는가.
   HTTP를 걷어내면 무엇이 무너지는가
3. **상태 보관 위치** — `dataHolder`의 정체. 전이 판정과 첫 사이클 무음을 어디에 붙일 것인가
4. **`/health/self` 현재 구현** — `lastCheckAgeSec` 계산 방식과 임계값
5. **설정 키 바인딩 방식** — `@ConfigurationProperties`인가 `${...}`인가.
   ⚠️ 이 프로젝트는 **yml은 camelCase, 코드는 kebab-case**여서 6주간 값이 안 닿은 전례가 있다.
   relaxed binding은 `@ConfigurationProperties`에만 적용되고 `${...}` 조회에는 적용되지 않는다.
   새로 추가하는 키도 같은 함정을 밟지 않는지 확인해라
6. **내가 위 3절에서 잘못 짚은 것이 있는가** — 있으면 지적해라

그다음 **변경 계획을 파일 단위로 제시**하고 내 승인을 받아라. 승인 전에 파일을 수정하지 마라.

---

## 5. 금지 사항

- ⚠️ **`git push` 금지.** push가 곧 배포다. 커밋까지만 하고 push는 내가 직접 한다
- ⚠️ **`git commit --amend` / `rebase` / force push 금지**
- 새 의존성 추가 전에 물어봐라. 4GB VPS라 메모리가 모든 결정의 전제다
- DB 스키마 변경 금지 (마이그레이션 수단 없음)
- 인증/보안 경로를 "임시로" 열지 마라
- 한 번에 여러 관심사를 섞지 마라. 위 4개 항목을 **커밋 단위로 분리**해라

---

## 6. 배포와 검증 — 내가 할 일

작성이 끝나면 아래 검증 절차를 함께 만들어 달라.

**배포 전 (로컬)**

- 컴파일 통과
- ⚠️ 로컬 Gradle 빌드가 Windows에서 `Could not move temporary workspace`로 실패하는 이슈가 있다.
  막히면 알려달라

**배포 후 (서버)**

```bash
# 판정 루프 생존
curl -s https://kyuhyeong.com/api/monitoring/health/self

# 컨테이너 로그로 전이 판정 확인
docker logs dashboard-app --since 5m | grep -i -E 'transition|expected|unexpected'

# quiz 자가 폴링이 실제로 사라졌는가 (제거 확인)
grep 'Apache-HttpClient' /var/log/nginx/access.log | tail -5
```

⚠️ **로그 침묵을 성공으로 읽지 마라.** 이 프로젝트에서 실제로 있었던 일이다 —
crash-loop 중이라 조용했던 것을 "수정 성공"으로 오판할 뻔했다.
로그가 없으면 **컨테이너가 살아 있는지 먼저** 본다.

**⚠️ 배포 실패 시나리오 (전례 있음)**

- GitHub Secret `DB_PASSWORD`가 서버 `.env`보다 1자 길어 crash-loop이 난 적이 있다(끝 공백).
  워크플로의 `export DB_PASSWORD=`가 `.env`를 이긴다
- 값을 노출하지 않고 진단하는 법:

```bash
docker exec dashboard-app printenv SPRING_DATASOURCE_PASSWORD | wc -c
grep '^DB_PASSWORD=' /root/dashboard/.env | cut -d= -f2- | wc -c
```

- 롤백: 커밋 해시 태그가 레지스트리에 있으므로 이전 태그로 재실행 가능

---

## 7. 이 프로젝트에서 이미 겪은 실패 — 반복하지 마라

| 사건 | 교훈 |
|---|---|
| 6주간의 거짓 초록 | 내려간 서비스가 UP으로 떠 있었다. 502를 받고도 `HttpStatusCodeException`을 UP으로 처리했다. **"체크할 수 없음"을 UP으로 위장하지 않는다.** `UNKNOWN`을 별도 상태로 둔다 |
| 설정 키 불일치 | yml camelCase vs 코드 kebab-case. 키를 못 찾아 기본값 10초로 6주간 폴링 |
| 07-27 기록 불일치 | 실행한 것과 목록만 만든 것이 한 문장에 섞여 "완료"가 됐다. **한 세션에서 실행한 것과 계획만 한 것을 구분해서 보고해라** |
| 스왑 원인 오진 | 대리 지표로 원인을 추론했다가 뒤집혔다. **원인을 세는 카운터를 찾아라** |
| 문서 드리프트 | 이틀 된 점검 문서를 근거로 판단했는데 이미 조치돼 있었다. **사실은 생성하고 판단만 기록한다** |

---

## 8. 첫 응답에서 해라

1. 4절의 조사 6항목 보고
2. 3절에서 내가 잘못 짚은 것 지적
3. 변경 계획 (파일 단위, 커밋 단위 분리)

**코드는 승인 후에.**
