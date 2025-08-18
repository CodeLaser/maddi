
plugins {
    `java-platform`
}

group = "io.codelaser"

repositories{
    mavenCentral()
}

dependencies {
    constraints {
        api("org.junit.jupiter:junit-jupiter-api:5.13.4")
        api("org.slf4j:slf4j-api:2.0.17")
        runtime("org.junit.jupiter:junit-jupiter-engine:5.13.4")
        runtime("ch.qos.logback:logback-classic:1.5.18")
    }
}

