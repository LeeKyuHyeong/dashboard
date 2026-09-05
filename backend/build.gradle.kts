plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
}

group = "com.kyuhyeong"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // System monitoring
    implementation("com.github.oshi:oshi-core:6.4.0")

    // Docker 는 docker CLI 서브프로세스로 호출한다(Dockerfile 에 docker-cli 설치).
    // docker-java 는 선언만 되어 있고 코드에서 쓰인 적이 없어 제거(2026-09-05).
    // 딸려오던 httpclient5 가 RestTemplate 의 기본 클라이언트로 잡혀
    // 감시 대상 nginx 로그에 "Apache-HttpClient/5.3.1" UA 를 남기던 경로도 함께 사라진다.

    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("com.h2database:h2")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
