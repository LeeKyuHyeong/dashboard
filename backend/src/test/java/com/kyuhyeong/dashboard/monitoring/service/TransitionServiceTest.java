package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.model.MonitoringInventory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransitionServiceTest {

    private final TransitionService service = new TransitionService();

    private MonitoringInventory inv(Map<String, String> states, String... unexpected) {
        return new MonitoringInventory(true, states, List.of(unexpected));
    }

    @Test
    void 첫_사이클은_전이로_취급하지_않는다() {
        List<String> t = service.evaluate(inv(Map.of("quiz-app", "DOWN")));

        // 부팅 직후 baseline 이 없다. 이미 죽어 있어도 알리지 않는다 —
        // crash-loop 이면 재시작마다 같은 알림이 반복되기 때문.
        assertThat(t).isEmpty();
    }

    @Test
    void 두번째_사이클부터_상태_변화를_전이로_잡는다() {
        service.evaluate(inv(Map.of("quiz-app", "UP")));

        List<String> t = service.evaluate(inv(Map.of("quiz-app", "DOWN")));

        assertThat(t).containsExactly("quiz-app UP -> DOWN");
    }

    @Test
    void 변화가_없으면_조용하다() {
        service.evaluate(inv(Map.of("quiz-app", "UP")));

        assertThat(service.evaluate(inv(Map.of("quiz-app", "UP")))).isEmpty();
    }

    @Test
    void 판정_불가_사이클은_기준선을_건드리지_않는다() {
        service.evaluate(inv(Map.of("quiz-app", "UP")));

        // docker 소켓이 끊긴 사이클
        assertThat(service.evaluate(MonitoringInventory.undecidable())).isEmpty();

        // 복구되고 보니 그사이 죽어 있었다 -> 놓치지 않고 잡아야 한다
        assertThat(service.evaluate(inv(Map.of("quiz-app", "DOWN"))))
                .containsExactly("quiz-app UP -> DOWN");
    }

    @Test
    void 판정_불가로_시작하면_기준선이_아직_없다() {
        assertThat(service.evaluate(MonitoringInventory.undecidable())).isEmpty();

        // 첫 판정 가능 사이클이 곧 기준선이므로 여전히 무음
        assertThat(service.evaluate(inv(Map.of("quiz-app", "DOWN")))).isEmpty();
    }

    @Test
    void 목록에_없는_컨테이너가_뜨고_지는_것을_잡는다() {
        service.evaluate(inv(Map.of("quiz-app", "UP")));

        assertThat(service.evaluate(inv(Map.of("quiz-app", "UP"), "stranger-app")))
                .containsExactly("unexpected 등장: stranger-app");

        assertThat(service.evaluate(inv(Map.of("quiz-app", "UP"))))
                .containsExactly("unexpected 사라짐: stranger-app");
    }

    @Test
    void 감시_대상_추가와_제외도_전이다() {
        service.evaluate(inv(Map.of("quiz-app", "UP")));

        assertThat(service.evaluate(inv(Map.of("quiz-app", "UP", "new-db", "UP"))))
                .containsExactly("new-db 감시 대상 추가 (UP)");

        assertThat(service.evaluate(inv(Map.of("quiz-app", "UP"))))
                .containsExactly("new-db 감시 대상에서 제외됨");
    }

    @Test
    void MISSING_과_DOWN_은_다른_전이다() {
        service.evaluate(inv(Map.of("quiz-app", "DOWN")));

        assertThat(service.evaluate(inv(Map.of("quiz-app", "MISSING"))))
                .containsExactly("quiz-app DOWN -> MISSING");
    }
}
