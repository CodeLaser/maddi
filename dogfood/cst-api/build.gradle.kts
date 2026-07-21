plugins {
    `java-library`
    id("org.e2immu.analyzer-plugin")
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

dependencies {
    // 'api', matching the real maddi-cst-api: cst-impl's own sources need these too, and only an api
    // dependency reaches its compile classpath -- and therefore its input configuration
    api(":maddi-support:0.8.2")
    api("org.slf4j:slf4j-api:2.0.17")
    api("org.jetbrains:annotations:26.1.0")
}

e2immu {
    // sourcePackages deliberately unset: see README
    jmods = "java.base"
}
