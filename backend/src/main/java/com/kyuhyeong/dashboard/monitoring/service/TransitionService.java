package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.model.MonitoringInventory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 사이클 간 <b>상태 전이</b>를 뽑아낸다. 알림(1-4)은 이 결과만 소비하면 된다.
 *
 * <p>기준선(baseline)은 화면에 노출되지 않는 판정 내부 상태라 {@link MonitoringDataHolder}
 * 가 아니라 이 서비스가 소유한다. 스케줄러 스레드 한 곳에서만 호출된다.
 *
 * <p><b>첫 사이클 무음</b>: 기준선은 인메모리라 재시작 때마다 날아간다. 그대로 두면
 * crash-loop 일 때 재시작마다 같은 알림이 반복된다. 그래서 부팅 후 첫 사이클은
 * 적재만 하고 전이로 취급하지 않는다.
 */
@Service
@Slf4j
public class TransitionService {

    /** null 이면 아직 기준선이 없다(부팅 직후). */
    private MonitoringInventory baseline;

    /**
     * @return 이번 사이클에서 발생한 전이 목록. 첫 사이클이거나 판정 불가면 빈 목록.
     */
    public List<String> evaluate(MonitoringInventory current) {
        // 판정 불가 사이클은 기준선을 건드리지 않는다. 여기서 기준선을 비우면
        // docker 가 잠깐 끊긴 사이에 죽은 컨테이너를 복구 후에도 영영 놓친다.
        if (!current.decidable()) {
            return List.of();
        }

        if (baseline == null) {
            baseline = current;
            log.info("전이 기준선 적재 — 첫 사이클은 전이로 취급하지 않는다 (현재 이상 {}건, unexpected {}건)",
                    current.notUp().size(), current.unexpected().size());
            return List.of();
        }

        List<String> transitions = new ArrayList<>();
        Map<String, String> before = baseline.expectedStates();
        Map<String, String> after = current.expectedStates();

        for (Map.Entry<String, String> e : after.entrySet()) {
            String prev = before.get(e.getKey());
            if (prev == null) {
                transitions.add(e.getKey() + " 감시 대상 추가 (" + e.getValue() + ")");
            } else if (!prev.equals(e.getValue())) {
                transitions.add(e.getKey() + " " + prev + " -> " + e.getValue());
            }
        }
        for (String name : before.keySet()) {
            if (!after.containsKey(name)) {
                transitions.add(name + " 감시 대상에서 제외됨");
            }
        }

        for (String name : diff(current.unexpected(), baseline.unexpected())) {
            transitions.add("unexpected 등장: " + name);
        }
        for (String name : diff(baseline.unexpected(), current.unexpected())) {
            transitions.add("unexpected 사라짐: " + name);
        }

        baseline = current;
        return transitions;
    }

    private List<String> diff(List<String> a, List<String> b) {
        Set<String> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return List.copyOf(result);
    }
}
