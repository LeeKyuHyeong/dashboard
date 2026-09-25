package com.kyuhyeong.dashboard.controller;

import com.kyuhyeong.dashboard.domain.Project;
import com.kyuhyeong.dashboard.dto.ProjectDetailDto;
import com.kyuhyeong.dashboard.dto.ProjectListDto;
import com.kyuhyeong.dashboard.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 숨긴 프로젝트(`visible=FALSE`)가 목록·상세 어느 쪽으로도 새지 않는지 고정한다.
 *
 * <p>2026-09-17 에 itsm 카드를 숨길 때 이 가드가 동작한다는 근거는 소스 한 줄
 * ({@code ProjectController.java:21}·{@code :30}) 과 운영 DB 조회뿐이었다.
 * 노출 가드인데 자동 테스트가 없어 O-005 로 등록됐고, 이 클래스가 그것을 닫는다.
 *
 * <p>목록 필터는 메서드 <b>이름</b>({@code findAllByVisibleTrueOrderBySortOrderAsc})에만 있다 —
 * Spring Data 가 런타임에 이름을 파싱해 SQL 을 만든다. 저장소를 mock 으로 바꾸면
 * 이름이 틀려도 테스트가 통과하므로, 여기서는 진짜 H2 + 진짜 파생 쿼리로 돌린다.
 * 컨트롤러도 실제 저장소를 주입해 만든다 — 확인하려는 것이 "두 경로 모두 막혔는가" 이기 때문이다.
 *
 * <p>시드({@code data-h2.sql})는 끈다. 고정값만 보기 위해서다.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // application.yml 의 기본 프로파일은 MariaDBDialect 다. @DataJpaTest 는 데이터소스만
        // H2 로 바꾸고 dialect 는 그대로 둬서, 그대로 두면 MariaDB 전용 DDL 을 H2 가 거부해
        // 테이블이 하나도 안 만들어진다(2026-09-26 실제로 6건 모두 이 이유로 실패).
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ProjectVisibilityTest {

    @Autowired
    private ProjectRepository projectRepository;

    private ProjectController controller;

    @BeforeEach
    void setUp() {
        controller = new ProjectController(projectRepository);

        save("보이는-둘째", true, 2);
        save("보이는-첫째", true, 1);
        save("숨긴-것", false, 3);
        save("visible-이-null", null, 4);
    }

    private void save(String slug, Boolean visible, int sortOrder) {
        projectRepository.save(Project.builder()
                .name(slug)
                .slug(slug)
                .description("설명")
                .techStack("[\"Spring Boot\"]")
                .visible(visible)
                .sortOrder(sortOrder)
                .build());
    }

    @Test
    void 목록은_보이는_것만_sortOrder_순서로_돌려준다() {
        List<ProjectListDto> list = controller.listProjects();

        assertThat(list).extracting(ProjectListDto::slug)
                .containsExactly("보이는-첫째", "보이는-둘째");
    }

    @Test
    void 숨긴_프로젝트는_목록에_나오지_않는다() {
        List<ProjectListDto> list = controller.listProjects();

        assertThat(list).extracting(ProjectListDto::slug)
                .doesNotContain("숨긴-것");
    }

    @Test
    void 숨긴_프로젝트는_slug_를_알아도_상세로_읽히지_않는다() {
        ResponseEntity<ProjectDetailDto> response = controller.getProject("숨긴-것");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void 보이는_프로젝트는_상세로_읽힌다() {
        ResponseEntity<ProjectDetailDto> response = controller.getProject("보이는-첫째");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().slug()).isEqualTo("보이는-첫째");
    }

    @Test
    void 없는_slug_는_404() {
        ResponseEntity<ProjectDetailDto> response = controller.getProject("그런-건-없다");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * 컬럼이 nullable 이라 {@code visible} 이 NULL 인 행이 생길 수 있다
     * (schema.sql 의 DEFAULT TRUE 는 값을 빼고 INSERT 할 때만 적용된다).
     * 목록은 {@code visible = true} 로 걸러 NULL 을 빼고,
     * 상세는 {@code Boolean.TRUE.equals(...)} 라 NULL 에서 NPE 없이 404 다.
     * 둘 다 "모르면 감춘다" 로 같은 방향이어야 한다.
     */
    @Test
    void visible_이_NULL_이면_목록에도_상세에도_나오지_않는다() {
        List<ProjectListDto> list = controller.listProjects();
        assertThat(list).extracting(ProjectListDto::slug)
                .doesNotContain("visible-이-null");

        ResponseEntity<ProjectDetailDto> response = controller.getProject("visible-이-null");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
