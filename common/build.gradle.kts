plugins {
    `java-library`
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

description = "common"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// 실행 애플리케이션이 아닌 라이브러리 모듈이므로 bootJar 대신 일반 jar를 만든다.
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework:spring-tx")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
