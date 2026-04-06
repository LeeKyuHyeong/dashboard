package com.kyuhyeong.dashboard.monitoring.controller;

import com.kyuhyeong.dashboard.monitoring.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseEmitterService.createEmitter();
    }

    @GetMapping("/logs/{containerName}")
    public String getLogs(
            @PathVariable String containerName,
            @RequestParam(defaultValue = "100") int tail
    ) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "logs", "--tail", String.valueOf(tail), containerName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output;
            }
            return "Failed to retrieve logs (exit code: " + exitCode + "): " + output;
        } catch (Exception e) {
            log.warn("Docker not available for logs: {}", e.getMessage());
            return "Docker not available";
        }
    }
}
