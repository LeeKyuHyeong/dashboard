package com.kyuhyeong.dashboard.monitoring.model;

import java.util.List;
import java.util.Map;

/**
 * 한 사이클의 <b>양방향 비교</b> 결과.
 *
 * @param decidable      docker 조회가 성공했는가. false 면 나머지 필드는 의미가 없다.
 * @param expectedStates 기대 목록 각각의 상태 (UP / DOWN / MISSING)
 * @param unexpected     <b>목록에 없는데 실행 중</b>인 컨테이너.
 *                       이 방향이 없으면 새 앱을 올리고 목록 갱신을 잊었을 때
 *                       감시에서 조용히 빠진다 — 단방향 비교의 사각지대.
 */
public record MonitoringInventory(
        boolean decidable,
        Map<String, String> expectedStates,
        List<String> unexpected
) {
    public static MonitoringInventory undecidable() {
        return new MonitoringInventory(false, Map.of(), List.of());
    }

    /** 기대 목록 중 UP 이 아닌 것들. */
    public List<String> notUp() {
        return expectedStates.entrySet().stream()
                .filter(e -> !"UP".equals(e.getValue()))
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .sorted()
                .toList();
    }

    public boolean hasAnomaly() {
        return !notUp().isEmpty() || !unexpected.isEmpty();
    }
}
