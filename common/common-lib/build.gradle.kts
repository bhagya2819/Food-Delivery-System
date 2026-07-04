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
    api("org.springframework:spring-web")
    api("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
