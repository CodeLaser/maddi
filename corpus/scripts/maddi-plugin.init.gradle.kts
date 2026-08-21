// Applies the maddi analyzer Gradle plugin to a corpus project WITHOUT editing its checkout, the way
// the `maven-plugin` route invokes the mojo by coordinate rather than adding it to the pom. Driven by
// catalogue.py's `gradle-plugin` route; see the comment there.
//
//   ./gradlew --init-script <this> -Dmaddi.pluginVersion=0.9.1 \
//             [-Dmaddi.pluginRepo=<dir>] [-Dmaddi.jmods=java.se,jdk.compiler] \
//             -Dmaddi.outputFile=<abs path> :some:module:maddi-write-input-configuration
//
// ⛔ Not editing the checkout is the point, not tidiness. A corpus entry declares what it writes into
// a third-party tree (`config.generates`), and that list feeds the pre-flight's preserve-list. A route
// that had to add a `plugins {}` block and a `pluginManagement {}` block would be writing into files
// that upstream OWNS, where `git reset --hard` -- which the corpus does use -- silently reverts it.
initscript {
    repositories {
        // `task corpus:config:plugin` publishes both build plugins here.
        mavenLocal()
        // ...and an explicit local file repository wins when one is given, which is what
        // `:maddi-gradleplugin:publishAllPublicationsToLocalPluginRepoRepository` fills.
        System.getProperty("maddi.pluginRepo")?.let { maven { url = uri(it) } }
        gradlePluginPortal()
    }
    dependencies {
        classpath("io.codelaser:maddi-gradleplugin:"
                + (System.getProperty("maddi.pluginVersion")
                        ?: error("-Dmaddi.pluginVersion is required")))
    }
}

// ⛔⛔ WHICH PROJECTS GET THE PLUGIN IS A CHOICE BETWEEN TWO DIFFERENT TESTS, and it was being made
// by accident. `allprojects` (the default, unchanged) means every sibling publishes its SOURCES on the
// `maddiSourceElements` variant, so the analysed module co-parses them -- the dogfood pattern. A
// single project means its siblings arrive as ordinary class-path entries, jars or classes
// directories, which is how a user who applies the plugin to their own project sees the world.
//
// They exercise DIFFERENT defects, and neither covers the other:
//   siblings as SOURCE     -> df0593929 (a sibling was handed the consumer's class path and scoping)
//   siblings as ARTIFACTS  -> c8fb38c03 (every sibling's part was named `main`, all but one dropped)
//
// ⚠ AND THEY DO NOT BOTH PARSE CLEAN. On pulsar `:managed-ledger`, siblings-as-source leaves 107
// diagnostics that are NOT a plugin defect: a sibling's `compileOnly` dependencies (swagger, findbugs
// annotations) are propagated to no consumer by Gradle at all, so they are absent rather than
// mis-scoped, and the only design that would close it is refuted (handoff §6b). A corpus entry that is
// permanently red is a gate people learn to ignore, so an entry picks its mode deliberately.
val applyTo: String = System.getProperty("maddi.applyTo") ?: "all"

allprojects {
    // afterEvaluate: a build script sets its source sets, and may register whole source sets, while it
    // is being evaluated. Applying before that would read the defaults instead of what the project
    // configured.
    afterEvaluate {
        if (!plugins.hasPlugin("java")) return@afterEvaluate
        if (applyTo != "all" && path != applyTo) return@afterEvaluate
        pluginManager.apply(io.codelaser.maddi.gradleplugin.AnalyzerPlugin::class.java)

        val extension = extensions.getByName("maddi")
                as io.codelaser.maddi.gradleplugin.AnalyzerExtension
        // Unset means the java.se closure (JavaModules.DEFAULT_JMODS), which is what the other routes
        // give the parse; catalogue.py maps an entry's `extra_jmods` onto this.
        System.getProperty("maddi.jmods")?.let { extension.jmods = it }

        // ⚠ The task writes to <project>/build/inputConfiguration.json by default, and a route that
        // copied it from there would have to GUESS the directory a Gradle project path maps to --
        // ':a:b' is 'a/b' only by convention, and settings.gradle.kts may say otherwise. Setting the
        // output directly removes the guess. configureEach is harmless across projects: only the task
        // actually requested on the command line runs, so only it writes.
        System.getProperty("maddi.outputFile")?.let { path ->
            tasks.withType(io.codelaser.maddi.gradleplugin.task.WriteInputConfigurationTask::class.java)
                    .configureEach { outputFile.set(File(path)) }
        }
    }
}
