
plugins {
    `java-platform`
}

dependencies {
    constraints {
        api("org.jgrapht:jgrapht-core:1.5.2")
        api("org.jgrapht:jgrapht-io:1.5.2")

        api("org.junit.jupiter:junit-jupiter-api:5.9.2")
        runtime("org.junit.jupiter:junit-jupiter-engine:5.9.2")

        api("org.slf4j:slf4j-api:2.0.17")
        runtime("ch.qos.logback:logback-classic:1.5.18")
    }
}

