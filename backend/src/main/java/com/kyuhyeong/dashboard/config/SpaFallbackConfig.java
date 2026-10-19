package com.kyuhyeong.dashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA 경로 fallback (O-012, 2026-10-19).
 *
 * <p>프론트는 {@code BrowserRouter} 라 {@code /projects/song-quiz} 같은 주소를 서버가 알지 못한다.
 * 앱 안에서 이동할 때는 서버 요청이 없어 멀쩡하지만, 새로고침·링크 공유로 직접 열면 Spring 404 였다.
 * 비-API·비-파일 경로를 {@code index.html} 로 돌려주면 React Router 가 나머지를 처리한다.
 *
 * <p>컨트롤러 catch-all({@code /{path}/**}) 로 만들지 않은 이유: 컨트롤러 매핑은 정적 리소스 핸들러보다
 * 우선이라 {@code /assets/*.js}·{@code /thumbnails/**} 까지 가로챈다. 그래서 정적 리소스 핸들러 자체에
 * "파일이 없으면 index.html" 규칙을 붙인다. 이 {@code /**} 핸들러가 등록되면 Boot 기본 핸들러는 생략된다.
 *
 * <p>규칙:
 * <ul>
 *   <li>{@code /api/**} — 컨트롤러가 먼저 받는다. 컨트롤러에 없는 API 경로는 여기 와도 404 로 남긴다
 *       (SPA 의 200 HTML 로 위장하면 배포 health 폴링·UptimeRobot 이 오판한다)</li>
 *   <li>확장자가 있는 경로({@code .png}·{@code .js} …) — 파일이 있으면 그 파일, 없으면 404</li>
 *   <li>그 밖의 경로 — {@code index.html}</li>
 * </ul>
 */
@Configuration
public class SpaFallbackConfig implements WebMvcConfigurer {

    static final String INDEX = "/static/index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaPathResourceResolver());
    }

    static class SpaPathResourceResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource requested = super.getResource(resourcePath, location);
            if (requested != null) {
                return requested;
            }
            if (!isSpaRoute(resourcePath)) {
                return null;
            }
            Resource index = new ClassPathResource(INDEX);
            return index.exists() ? index : null;
        }
    }

    /** API 가 아니고 마지막 세그먼트에 확장자가 없는 경로만 SPA 라우트로 본다. */
    static boolean isSpaRoute(String resourcePath) {
        if (resourcePath.startsWith("api/") || resourcePath.equals("api")) {
            return false;
        }
        int lastSlash = resourcePath.lastIndexOf('/');
        String last = resourcePath.substring(lastSlash + 1);
        return !last.contains(".");
    }
}
