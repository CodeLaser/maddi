package io.codelaser.maddi.inspection.mixed

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * The stub directories of the mixed inspectors, deleted when the JVM exits, by ONE shutdown hook for all of them.
 *
 * ⛔ A hook per inspector kept every inspector alive until the JVM exited: inside the class, `runCatching { }` in the
 * hook's lambda resolved to the extension `this.runCatching`, so the lambda captured the inspector (javap:
 * `invokedynamic run:(MixedProjectInspector;Path)`), and with it its Settings and everything they reach. On the
 * refactor server every model load added one (7 ProjectImpl for 1 ProjectDataImpl in a heap dump). The registry holds
 * paths only.
 */
internal object StubDirectories {
    private val directories: MutableSet<Path> = ConcurrentHashMap.newKeySet()

    init {
        Runtime.getRuntime().addShutdownHook(Thread(::deleteAll, "maddi-stub-directories"))
    }

    /** A new temporary directory, deleted at JVM exit. */
    fun create(prefix: String): Path = Files.createTempDirectory(prefix).also { directories.add(it) }

    private fun deleteAll() {
        for (dir in directories) {
            kotlin.runCatching { dir.toFile().deleteRecursively() }
        }
    }
}
