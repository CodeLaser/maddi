
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    api(platform(project(":platform")))

    // common logging
    implementation("org.slf4j:slf4j-api")
    testRuntimeOnly("ch.qos.logback:logback-classic")

    // common test
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

