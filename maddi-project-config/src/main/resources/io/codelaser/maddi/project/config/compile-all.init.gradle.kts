// maddi-project-config: `maddiCompileAll` compiles every Java source set of the build -- main, test, testFixtures and any
// other a project declares, which no single task name covers -- so that javac's arguments for each appear in the
// debug log, where Gradle writes them ("Compiler arguments: ..."). The configuration is made from those lines.
//
// A compile task that is up to date logs no arguments: every JavaCompile runs. The build is run with
// --no-build-cache too, as a task taken from the cache logs none either.
//
// Kotlin, not Groovy: the Groovy of a Gradle that runs on a newer JDK than it knows cannot compile a script at all
// ("Unsupported class file major version"), where the Kotlin DSL can.
allprojects {
    tasks.withType(JavaCompile::class.java).configureEach { outputs.upToDateWhen { false } }
    val compileAll = tasks.register("maddiCompileAll") {
        description = "Compiles every Java source set of this project and its subprojects (maddi)."
        dependsOn(tasks.withType(JavaCompile::class.java))
    }
    if (this == rootProject) {
        // by path: a subproject's task is not registered yet while the root is configured
        compileAll.configure { dependsOn(subprojects.map { it.path + ":maddiCompileAll" }) }
    }
}
