package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.model.MonitoringData;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 팬아웃. <b>판정 루프 스레드에서 분리한다.</b>
 *
 * <p>반쯤 끊긴 TCP 로의 {@code emitter.send()} 는 IOException 을 던지지 않고 그냥 멈춘다.
 * 이전 구현은 이 호출을 스케줄러 스레드(scheduling-1) 위에서 순차로 돌렸기 때문에
 * 브라우저 탭 하나가 판정 루프 전체를 정지시킬 수 있었다.
 *
 * <p>풀 크기를 늘리는 것으로는 부족하다 — 블로킹 emitter 가 풀 크기만큼이면 같은 상태가 된다.
 * 그래서 <b>스레드 1개 + 대기열 없음 + 밀리면 그 사이클을 버린다.</b> 다음 사이클에 최신값이
 * 어차피 다시 나가므로 밀린 브로드캐스트를 쌓아둘 이유가 없다.
 * 막힌 스레드는 emitter 타임아웃(유한값)이 만료되면 풀린다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SseEmitterService {

    /** 무한대(Long.MAX_VALUE)로 두면 onTimeout 이 영원히 발화하지 않아 막힌 연결이 영구히 남는다. */
    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(5).toMillis();

    /** 무인증 공개 경로다. 상한이 없으면 연결만으로 메모리를 밀어낼 수 있다. */
    private static final int MAX_EMITTERS = 40;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final MonitoringDataHolder dataHolder;
    private final AtomicLong dropped = new AtomicLong();

    private final ThreadPoolExecutor broadcastExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r, "sse-broadcast");
                t.setDaemon(true);
                return t;
            },
            (r, executor) -> log.warn("이전 브로드캐스트가 아직 안 끝나 이번 사이클을 버린다 (누적 {}회)",
                    dropped.incrementAndGet()));

    public SseEmitter createEmitter() {
        if (emitters.size() >= MAX_EMITTERS) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SSE 연결 상한 초과");
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);

        sendInitial(emitter);
        return emitter;
    }

    /** 스케줄러 스레드에서 호출된다. 절대 블로킹하지 않는다. */
    public void broadcast() {
        try {
            broadcastExecutor.execute(this::doBroadcast);
        } catch (Exception e) {
            long n = dropped.incrementAndGet();
            log.warn("브로드캐스트 제출 실패 — 이번 사이클 폐기 (누적 {}회): {}", n, e.getMessage());
        }
    }

    private void doBroadcast() {
        MonitoringData data = dataHolder.getAll();
        for (SseEmitter emitter : emitters) {
            // emitter 하나의 실패가 나머지를 막지 않도록 개별로 격리한다.
            // IOException 만 잡던 이전 구현은 완료된 emitter 의 IllegalStateException 에
            // for 루프가 통째로 중단돼 뒤쪽 구독자가 그 사이클을 받지 못했다.
            try {
                emitter.send(SseEmitter.event().name("monitoring").data(data));
            } catch (Exception e) {
                emitters.remove(emitter);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // 이미 완료된 emitter
                }
                log.debug("죽은 SSE 연결 제거: {}", e.getMessage());
            }
        }
    }

    private void sendInitial(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("monitoring").data(dataHolder.getAll()));
        } catch (Exception e) {
            emitters.remove(emitter);
            log.warn("SSE 최초 이벤트 전송 실패: {}", e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        broadcastExecutor.shutdownNow();
    }
}
