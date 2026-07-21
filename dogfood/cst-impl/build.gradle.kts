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
        java { setSrcDirs(listOf("../../maddi-cst-impl/src/main/java")) }
        resources { setSrcDirs(emptyList<String>()) }
    }
}

dependencies {
    // the project dependency is the one under test: cst-api must arrive as SOURCE, not as a jar
    implementation(project(":cst-api"))
    // below the pair under test, so still jars; maddi-support in particular stays a jar so that reading
    // @Mark/@Only out of byte code is exercised
    implementation(":maddi-cst-analysis:0.8.2")
    implementation(":maddi-util:0.8.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.jetbrains:annotations:26.1.0")
}

e2immu {
    // sourcePackages deliberately unset: see README
    jmods = "java.base"
}
