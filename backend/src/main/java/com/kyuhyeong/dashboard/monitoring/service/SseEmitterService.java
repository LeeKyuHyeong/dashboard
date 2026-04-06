package com.kyuhyeong.dashboard.monitoring.service;

import com.kyuhyeong.dashboard.monitoring.model.MonitoringData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseEmitterService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final MonitoringDataHolder dataHolder;

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        emitters.add(emitter);

        // Send initial data immediately
        sendInitial(emitter);

        return emitter;
    }

    public void broadcast() {
        MonitoringData data = dataHolder.getAll();
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("monitoring")
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.debug("Removing dead SSE emitter: {}", e.getMessage());
            }
        }

        emitters.removeAll(deadEmitters);
    }

    private void sendInitial(SseEmitter emitter) {
        try {
            MonitoringData data = dataHolder.getAll();
            emitter.send(SseEmitter.event()
                    .name("monitoring")
                    .data(data));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE data: {}", e.getMessage());
        }
    }
}
