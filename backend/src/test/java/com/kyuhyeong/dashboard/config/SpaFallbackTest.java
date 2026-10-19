package com.kyuhyeong.dashboard.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 상세 URL 을 직접 열면(새로고침·링크 공유) Spring 404 였다 (O-012, 2026-10-19 배포 Smoke 에서 발견).
 * 운영 jar 의 {@code static/} 은 Dockerfile 이 Vite dist 를 복사해 만들므로 테스트 클래스패스에는 없다 —
 * {@code src/test/resources/static/} 에 최소 {@code index.html}·{@code assets/app.js} 를 둔다.
 *
 * <p>핵심은 세 경계다: SPA 라우트는 index.html, 실제 파일은 그 파일, {@code /api/**} 와 없는 파일은 404.
 * 셋 중 하나라도 무너지면 "화면은 뜨는데 health 가 HTML 을 받는다" 류의 조용한 오판이 생긴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SpaFallbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 상세_URL_을_직접_열면_index_html_을_돌려준다() throws Exception {
        mockMvc.perform(get("/projects/song-quiz"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("spa-fallback-test-marker")));
    }

    @Test
    void 깊은_경로와_루트도_index_html_이다() throws Exception {
        mockMvc.perform(get("/projects/song-quiz/extra"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("spa-fallback-test-marker")));
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("spa-fallback-test-marker")));
    }

    @Test
    void 실제_정적_파일은_index_가_아니라_그_파일이다() throws Exception {
        String body = mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("spa-fallback-asset").doesNotContain("spa-fallback-test-marker");
    }

    @Test
    void 없는_파일_경로는_404_로_남긴다() throws Exception {
        mockMvc.perform(get("/thumbnails/missing.png")).andExpect(status().isNotFound());
        mockMvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
    }

    @Test
    void 컨트롤러에_없는_API_경로는_HTML_로_위장하지_않고_404_다() throws Exception {
        mockMvc.perform(get("/api/nope")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void 규칙_단위_점검() {
        assertThat(SpaFallbackConfig.isSpaRoute("projects/song-quiz")).isTrue();
        assertThat(SpaFallbackConfig.isSpaRoute("")).isTrue();
        assertThat(SpaFallbackConfig.isSpaRoute("v1.2/page")).isTrue();      // 확장자는 마지막 세그먼트만 본다
        assertThat(SpaFallbackConfig.isSpaRoute("api")).isFalse();
        assertThat(SpaFallbackConfig.isSpaRoute("api/projects")).isFalse();
        assertThat(SpaFallbackConfig.isSpaRoute("thumbnails/x.png")).isFalse();
        assertThat(SpaFallbackConfig.isSpaRoute("apiary")).isTrue();         // "api" 접두 오탐 없음
    }
}
