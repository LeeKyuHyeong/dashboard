package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.config.MonitoringProperties;
import com.kyuhyeong.dashboard.monitoring.model.ServiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 판정이 {@code State.Status} 만 읽어, 컨테이너가 떠 있기만 하면 UP 이었다 (2026-09-19 발견).
 * HEALTHCHECK 가 unhealthy 를 보고해도 — DB 가 접속을 못 받거나 앱이 응답을 못 해도 — 화면은 UP 이고 전이도 남지 않았다.
 * HEALTHCHECK 가 있는 컨테이너는 그 결과를 판정에 넣는다. 없는 컨테이너는 이전과 같다.
 */
class HealthCheckServiceHealthTest {

    /** {이름, 상태, StartedAt, health} 를 실제 inspect 출력 형식으로 돌려주는 가짜 docker. */
    private static class FakeDocker extends DockerCli {
        private final List<String[]> containers;

        FakeDocker(List<String[]> containers) {
            this.containers = containers;
        }

        @Override
        public Result exec(List<String> command, Duration timeout) {
            if (command.contains("inspect")) {
                String out = containers.stream()
                        .map(c -> "/" + c[0] + "\t" + c[1] + "\t" + c[2] + "\t" + c[3])
                        .reduce("", (a, b) -> a + b + "\n");
                return new Result(true, false, 0, out, "");
            }
            String out = containers.stream().map(c -> c[0]).reduce("", (a, b) -> a + b + "\n");
            return new Result(true, false, 0, out, "");
        }
    }

    private MonitoringProperties props;

    @BeforeEach
    void setUp() {
        props = new MonitoringProperties();

        MonitoringProperties.ServiceConfig quiz = new MonitoringProperties.ServiceConfig();
        quiz.setName("Song Quiz");
        quiz.setProjectSlug("song-quiz");
        quiz.setContainerName("quiz-app");
        props.setServices(List.of(quiz));

        props.setExpected(List.of("quiz-app", "quiz-db", "account-api"));
        props.setGroups(Map.of("quiz-app", List.of("quiz-app-blue", "quiz-app-green")));
    }

    private MonitoringDataHolder run(String[]... containers) {
        MonitoringDataHolder holder = new MonitoringDataHolder(props);
        new HealthCheckService(props, holder, new FakeDocker(List.of(containers))).checkAll();
        return holder;
    }

    @Test
    void running_이고_healthy_면_UP() {
        MonitoringDataHolder holder = run(
                new String[]{"quiz-app-green", "running", "2026-09-08T10:00:00Z", "healthy"},
                new String[]{"quiz-db", "running", "2026-09-07T22:00:00Z", "healthy"},
                new String[]{"account-api", "running", "2026-09-07T22:00:00Z", "none"});

        assertThat(holder.getAll().services().get(0).getStatus()).isEqualTo("UP");
        assertThat(holder.getInventory().hasAnomaly()).isFalse();
    }

    @Test
    void running_이어도_unhealthy_면_DOWN_이고_이유가_카드에_보인다() {
        MonitoringDataHolder holder = run(
                new String[]{"quiz-app-green", "running", "2026-09-08T10:00:00Z", "unhealthy"},
                new String[]{"quiz-db", "running", "2026-09-07T22:00:00Z", "unhealthy"},
                new String[]{"account-api", "running", "2026-09-07T22:00:00Z", "none"});

        ServiceStatus quiz = holder.getAll().services().get(0);
        assertThat(quiz.getStatus()).isEqualTo("DOWN");
        // DOWN 하나로 합쳤으므로 "죽었다"와 "떠 있는데 응답을 못 한다"는 이 칸으로 구분한다
        assertThat(quiz.getDockerStatus()).isEqualTo("unhealthy");
        assertThat(holder.getInventory().expectedStates())
                .containsEntry("quiz-app", "DOWN")
                .containsEntry("quiz-db", "DOWN")
                .containsEntry("account-api", "UP");
        assertThat(holder.getInventory().hasAnomaly()).isTrue();
    }

    @Test
    void HEALTHCHECK_가_없는_컨테이너는_이전처럼_running_이면_UP() {
        MonitoringDataHolder holder = run(
                new String[]{"quiz-app-green", "running", "2026-09-08T10:00:00Z", "healthy"},
                new String[]{"quiz-db", "running", "2026-09-07T22:00:00Z", "healthy"},
                new String[]{"account-api", "running", "2026-09-07T22:00:00Z", "none"});

        assertThat(holder.getInventory().expectedStates()).containsEntry("account-api", "UP");
    }

    @Test
    void starting_은_UP_으로_본다_배포_때마다_이상_전이가_찍히지_않게() {
        // quiz 는 배포마다 새 색이 start_period(60초) 동안 starting 이다
        MonitoringDataHolder holder = run(
                new String[]{"quiz-app-blue", "running", "2026-09-08T09:00:00Z", "healthy"},
                new String[]{"quiz-app-green", "running", "2026-09-08T10:00:00Z", "starting"},
                new String[]{"quiz-db", "running", "2026-09-07T22:00:00Z", "healthy"},
                new String[]{"account-api", "running", "2026-09-07T22:00:00Z", "none"});

        ServiceStatus quiz = holder.getAll().services().get(0);
        assertThat(quiz.getContainerName()).isEqualTo("quiz-app-green");
        assertThat(quiz.getStatus()).isEqualTo("UP");
        assertThat(holder.getInventory().hasAnomaly()).isFalse();
    }

    @Test
    void 대기_색이_Exited_여도_활성_색이_healthy_면_UP() {
        MonitoringDataHolder holder = run(
                new String[]{"quiz-app-green", "running", "2026-09-08T10:00:00Z", "healthy"},
                new String[]{"quiz-app-blue", "exited", "2026-09-08T09:00:00Z", "unhealthy"},
                new String[]{"quiz-db", "running", "2026-09-07T22:00:00Z", "healthy"},
                new String[]{"account-api", "running", "2026-09-07T22:00:00Z", "none"});

        assertThat(holder.getInventory().expectedStates()).containsEntry("quiz-app", "UP");
        assertThat(holder.getInventory().hasAnomaly()).isFalse();
    }

    @Test
    void 멈춘_컨테이너는_health_와_무관하게_DOWN_이고_상태_칸은_docker_상태_그대로다() {
        MonitoringDataHolder holder = run(
                new String[]{"quiz-app-green", "exited", "2026-09-08T10:00:00Z", "unhealthy"},
                new String[]{"quiz-db", "running", "2026-09-07T22:00:00Z", "healthy"},
                new String[]{"account-api", "running", "2026-09-07T22:00:00Z", "none"});

        ServiceStatus quiz = holder.getAll().services().get(0);
        assertThat(quiz.getStatus()).isEqualTo("DOWN");
        assertThat(quiz.getDockerStatus()).isEqualTo("exited");
    }

    @Test
    void inspect_템플릿은_HEALTHCHECK_가_없는_컨테이너에서도_행을_낸다() {
        // {{.State.Health.Status}} 를 그대로 쓰면 Health 가 nil 인 컨테이너에서 템플릿이 에러를 내고
        // 그 컨테이너의 행이 빠져 MISSING 으로 오판된다. nil 가드가 템플릿에 있어야 한다.
        List<List<String>> commands = new ArrayList<>();
        DockerCli recording = new DockerCli() {
            @Override
            public Result exec(List<String> command, Duration timeout) {
                commands.add(command);
                return new Result(true, false, 0, command.contains("inspect") ? "" : "quiz-db\n", "");
            }
        };
        new HealthCheckService(props, new MonitoringDataHolder(props), recording).checkAll();

        List<String> inspect = commands.stream().filter(c -> c.contains("inspect")).findFirst().orElseThrow();
        assertThat(inspect.get(3))
                .contains("{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}");
    }
}
