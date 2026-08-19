plugins {
    `java-library`
    id("io.codelaser.maddi.analyzer")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

sourceSets {
    main {
        java { setSrcDirs(listOf("../../maddi-cst-api/src/main/java")) }
        resources { setSrcDirs(emptyList<String>()) }
    }
}

val maddiVersion = extra["maddiVersion"] as String // the project's own version; see settings.gradle.kts

dependencies {
    // 'api', matching the real maddi-cst-api: cst-impl's own sources need these too, and only an api
    // dependency reaches its compile classpath -- and therefore its input configuration
    api(":maddi-annotation:$maddiVersion")
    api(":maddi-support:$maddiVersion")
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.jetbrains:annotations:26.1.0")
}

maddi {
    // sourcePackages deliberately unset: see README
    jmods = "java.base"
}
