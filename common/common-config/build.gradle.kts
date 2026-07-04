plugins {
    `java-library`
    alias(libs.plugins.spring.dependency.management)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}")
    }
}

dependencies {
    api(project(":common:common-lib"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.github.openfeign:feign-core")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
