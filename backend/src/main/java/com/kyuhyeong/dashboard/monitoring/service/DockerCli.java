package com.kyuhyeong.dashboard.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * docker CLI 서브프로세스 호출. <b>모든 호출에 유한 타임아웃이 걸린다.</b>
 *
 * <p>출력을 파이프로 읽지 않고 임시 파일로 리다이렉트한다. 파이프를 쓰면 둘 중 하나에 걸린다.
 * <ul>
 *   <li>{@code waitFor()} 를 먼저 하면 — 출력이 파이프 버퍼(64KB)를 넘는 순간 자식이 멈춰 데드락</li>
 *   <li>{@code readAllBytes()} 를 먼저 하면 — 자식이 스트림을 닫지 않는 한 <b>영원히</b> 블로킹.
 *       타임아웃 줄에 도달조차 못 한다(이 프로젝트의 직전 구현이 이 상태였다)</li>
 * </ul>
 * 파일 리다이렉트는 자식이 블로킹되지 않으므로 {@code waitFor(timeout)} 하나로 끝난다.
 */
@Component
@Slf4j
public class DockerCli {

    /** @param timedOut 타임아웃으로 강제 종료됐는가. stdout 은 그때까지 쓰인 부분만 담긴다. */
    public record Result(boolean started, boolean timedOut, int exitCode, String stdout, String stderr) {

        public boolean ok() {
            return started && !timedOut && exitCode == 0;
        }

        public List<String> lines() {
            if (stdout == null || stdout.isBlank()) return List.of();
            return stdout.lines().map(String::trim).filter(l -> !l.isEmpty()).toList();
        }

        public String diagnostic() {
            if (!started) return "docker 실행 불가: " + stderr;
            if (timedOut) return "docker 응답 없음(타임아웃)";
            return "exit " + exitCode + (stderr == null || stderr.isBlank() ? "" : " / " + stderr.trim());
        }
    }

    public Result exec(List<String> cmd, Duration timeout) {
        Path out = null;
        Path err = null;
        Process p = null;
        try {
            out = Files.createTempFile("docker-out", ".tmp");
            err = Files.createTempFile("docker-err", ".tmp");

            // stderr 를 stdout 에 섞지 않는다 — 진단 문자열이 데이터를 오염시킨다.
            p = new ProcessBuilder(cmd)
                    .redirectOutput(out.toFile())
                    .redirectError(err.toFile())
                    .start();

            boolean exited = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                p.destroyForcibly();
                return new Result(true, true, -1, read(out), read(err));
            }
            return new Result(true, false, p.exitValue(), read(out), read(err));
        } catch (InterruptedException e) {
            if (p != null) p.destroyForcibly();
            Thread.currentThread().interrupt();
            return new Result(false, false, -1, "", "interrupted");
        } catch (Exception e) {
            if (p != null) p.destroyForcibly();
            return new Result(false, false, -1, "", String.valueOf(e.getMessage()));
        } finally {
            delete(out);
            delete(err);
        }
    }

    private String read(Path path) {
        try {
            return path == null ? "" : Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }

    private void delete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.debug("임시 파일 삭제 실패 {}: {}", path, e.getMessage());
        }
    }
}
