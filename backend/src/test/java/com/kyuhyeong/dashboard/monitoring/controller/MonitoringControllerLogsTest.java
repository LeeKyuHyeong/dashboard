package com.kyuhyeong.dashboard.monitoring.controller;

import com.kyuhyeong.dashboard.monitoring.config.MonitoringProperties;
import com.kyuhyeong.dashboard.monitoring.service.DockerCli;
import com.kyuhyeong.dashboard.monitoring.service.MonitoringDataHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로그 조회는 경로의 이름을 그대로 {@code docker logs} 에 넘겼다 (2026-09-19 발견).
 * 인자 목록으로 실행하므로 셸 주입은 안 되지만 ① 호스트의 아무 컨테이너(DB·fail2ban 포함) 로그를 읽을 수 있고
 * ② {@code -f} 같은 값은 docker 옵션으로 해석돼 타임아웃까지 워커 스레드를 붙잡는다.
 * 운영은 nginx 가 이 경로를 통째로 막고 있지만(404), 그 한 줄에만 기대지 않도록 감시 대상 이름만 받는다.
 */
class MonitoringControllerLogsTest {

    private final List<List<String>> executed = new ArrayList<>();
    private MonitoringController controller;

    @BeforeEach
    void setUp() {
        MonitoringProperties props = new MonitoringProperties();
        props.setExpected(List.of("quiz-app", "quiz-db", "account-api"));
        props.setGroups(Map.of("quiz-app", List.of("quiz-app-blue", "quiz-app-green")));
        props.setIgnored(List.of("house-db"));

        DockerCli docker = new DockerCli() {
            @Override
            public Result exec(List<String> command, Duration timeout) {
                executed.add(command);
                return new Result(true, false, 0, "log line\n", "");
            }
        };
        controller = new MonitoringController(null, props, new MonitoringDataHolder(props), docker);
    }

    @Test
    void 감시_대상_컨테이너는_로그를_돌려준다() {
        ResponseEntity<String> response = controller.getLogs("account-api", 100);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("log line");
        assertThat(executed).containsExactly(List.of("docker", "logs", "--tail", "100", "account-api"));
    }

    @Test
    void 그룹_멤버의_실제_이름은_받는다_화면이_보내는_이름이다() {
        assertThat(controller.getLogs("quiz-app-green", 100).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void 논리_이름은_실제_컨테이너가_아니므로_거부한다() {
        assertThat(controller.getLogs("quiz-app", 100).getStatusCode().value()).isEqualTo(404);
        assertThat(executed).isEmpty();
    }

    @Test
    void 감시_대상이_아닌_이름은_docker_를_실행하지_않고_404() {
        for (String name : List.of("house-db", "some-other-container", "-f", "--since=1h", "--details", "")) {
            ResponseEntity<String> response = controller.getLogs(name, 100);

            assertThat(response.getStatusCode().value()).as(name).isEqualTo(404);
            assertThat(response.getBody()).as("거부 응답에 컨테이너 이름을 담지 않는다")
                    .doesNotContain("quiz").doesNotContain("account");
        }
        assertThat(executed).isEmpty();
    }

    @Test
    void tail_범위_제한은_그대로다() {
        controller.getLogs("quiz-db", 999999);
        controller.getLogs("quiz-db", -5);

        assertThat(executed.get(0)).containsSequence("--tail", "1000");
        assertThat(executed.get(1)).containsSequence("--tail", "1");
    }
}
