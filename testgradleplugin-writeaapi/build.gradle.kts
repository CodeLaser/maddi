plugins {
    java
    id("org.e2immu.analyzer-plugin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
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

// we're not executing the tests in "test", they're there to provide .class files to analyze!!

tasks.check {
    dependsOn(functionalTestTask)
}

project.group = "io.codelaser.build"

e2immu {
    jmods = "java.base.jmod"
    debugTargets = "classpath"
    annotatedApiTargetDir = "/tmp/testWriteAnnotatedAPIDir"
    annotatedApiPackages = "java.util.,com.foo"
    annotatedApiTargetPackage = "org.e2immu.testwrite"
}

// no logic for publishing!
