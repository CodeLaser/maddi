package org.e2immu.analyzer.run.config.compile;

import java.util.List;

/**
 * The language-independent view of a single compiler invocation (one {@code javac}/{@code kotlinc} command
 * line) that {@link CompileListToSourceSets} needs to reconstruct a source-set graph. Implemented by
 * {@code Javac} (openjdk) and {@code Kotlinc} (kotlin).
 *
 * <p>The core identity is the {@link #destination()} (the {@code -d} output location): another invocation whose
 * {@link #classpath()} or {@link #modulePath()} contains that destination gets a dependency edge to it.
 *
 * <p>{@link #moduleName()} and {@link #friendPaths()} are Kotlin-provided signals; Java implementations return
 * {@code null} / an empty list.
 */
public interface CompileInvocation {

    /** The {@code -d} output location (a directory, or for kotlinc possibly a {@code .jar}); the identity key. */
    String destination();

    /** Classpath entries (jars and class directories); {@code null} if none given. */
    List<String> classpath();

    /** Module-path entries; {@code null} or empty for kotlinc (Kotlin/JVM uses the classpath). */
    List<String> modulePath();

    /** Explicit source roots ({@code -sourcepath}); empty for kotlinc, which has no such option. */
    List<String> sourcePath();

    /** The individual source files passed on the command line ({@code .java}/{@code .kt}). */
    List<String> sourceFiles();

    /** The source encoding, or {@code null} for the default. */
    String encoding();

    /** The compiler's module name ({@code -module-name}); {@code null} for javac. */
    default String moduleName() {
        return null;
    }

    /**
     * Kotlin friend-path outputs ({@code -Xfriend-paths}): the output locations this invocation is allowed to
     * see {@code internal} members of — in practice a test source set pointing at its main output. Empty for
     * javac. Resolved to dependency edges (and marks this set as a test set).
     */
    default List<String> friendPaths() {
        return List.of();
    }
}
