plugins {
    java
    id("org.e2immu.analyzer-plugin")
}

group = "io.codelaser.build"

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java", "src/main2/java"))
        }
    }
}

repositories {
    maven {
        url = uri(project.findProperty("codeartifactPublicUri") as String)
        credentials {
            username = "aws"
            password = project.findProperty("codeartifactToken") as String
        }
    }
    mavenCentral()
}

// see https://docs.gradle.org/current/userguide/testing_gradle_plugins.html
val functionalTest: SourceSet by sourceSets.creating

val jupiterApiVersion = project.findProperty("jupiterApiVersion") as String
val jupiterEngineVersion = project.findProperty("jupiterEngineVersion") as String

dependencies {
    implementation(gradleApi())
    implementation("io.codelaser:maddi-support:$version")
    implementation("io.codelaser:maddi-util:$version")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterApiVersion") // used in tests -> auto
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterEngineVersion")

    "functionalTestImplementation"("org.junit.jupiter:junit-jupiter-api:$jupiterApiVersion")
    "functionalTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:$jupiterEngineVersion")
    "functionalTestImplementation"(gradleTestKit())
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    dependsOn(tasks.getByName("e2immu-analyzer"))

    description = "Runs the functional tests."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    mustRunAfter(tasks.test)
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
}

tasks.check {
    dependsOn(functionalTestTask)
}

project.group = "io.codelaser.build"

e2immu {
    workingDirectory = ""
    jmods = "java.base,java.xml"
    debugTargets = "classpath"
    sourcePackages = "org.e2immu.something.,org.e2immu.test.main"
    testSourcePackages = "org.e2immu.test.test"
    analyzedAnnotatedApiDirs = "../analyzer-shallow/e2immu-shallow-aapi/src/main/resources/json"
    excludeFromClasspath = "gradle-api-8.9.jar" // because of javax.xml.parsers.*
}

// no logic for publishing!

