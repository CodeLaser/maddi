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
        java { setSrcDirs(listOf("../../maddi-cst-analysis/src/main/java")) }
        resources { setSrcDirs(emptyList<String>()) }
    }
}

dependencies {
    // cst-analysis implements the Value/Property interfaces from cst-api; 'api' so it reaches cst-impl too.
    // The plugin now wires this transitive cst-analysis -> cst-api source edge into the input configuration.
    api(project(":cst-api"))
    // support stays a jar (byte-code @Mark/@Only reading is exercised there)
    implementation(":maddi-support:0.8.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.jetbrains:annotations:26.1.0")
}

e2immu {
    // sourcePackages deliberately unset: see README
    jmods = "java.base"
}
