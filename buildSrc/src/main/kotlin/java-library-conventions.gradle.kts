
plugins {
    `java-library`
}

tasks.withType<Test> {
    useJUnitPlatform()
}

group = "io.codelaser"

dependencies {
    api(platform(project(":platform")))

    // common logging
    implementation("org.slf4j:slf4j-api")
    testRuntimeOnly("ch.qos.logback:logback-classic")

    // common test
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.jetbrains:annotations")

}

