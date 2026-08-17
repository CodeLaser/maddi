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
        java { setSrcDirs(listOf("../../maddi-cst-analysis/src/main/java")) }
        resources { setSrcDirs(emptyList<String>()) }
    }
}

val maddiVersion = extra["maddiVersion"] as String // the project's own version; see settings.gradle.kts

dependencies {
    // cst-analysis implements the Value/Property interfaces from cst-api; 'api' so it reaches cst-impl too.
    // The plugin now wires this transitive cst-analysis -> cst-api source edge into the input configuration.
    api(project(":cst-api"))
    // support stays a jar (byte-code @Mark/@Only reading is exercised there)
    implementation(":maddi-support:$maddiVersion")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.jetbrains:annotations:26.1.0")
}

e2immu {
    // sourcePackages deliberately unset: see README
    jmods = "java.base"
}
