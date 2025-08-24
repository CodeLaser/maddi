
plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("org.jgrapht:jgrapht-core:1.5.2")
        api("org.jgrapht:jgrapht-io:1.5.2")

        api("org.junit.jupiter:junit-jupiter-api:5.13.0")

        api("org.slf4j:slf4j-api:2.0.17")
        api("ch.qos.logback:logback-classic:1.5.18")

        api("org.jetbrains:annotations:26.0.2")
        api("com.fasterxml.jackson.core:jackson-databind:2.19.2")
        api("commons-cli:commons-cli:1.10.0")
    }
}

