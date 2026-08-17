package io.codelaser.maddi.run.config.compile;

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

    /** javac's {@code --release}; {@code <= 0} when the invocation did not pass one. */
    default int release() {
        return 0;
    }

    /** javac's {@code -source}; {@code <= 0} when the invocation did not pass one. */
    default int sourceRelease() {
        return 0;
    }

    /**
     * The Java API level this invocation compiled against, or {@code <= 0} when it said nothing.
     *
     * <p>⛔ It was parsed and then thrown away. {@code Javac} has held {@code release}, {@code sourceRelease} and
     * {@code targetRelease} since it was written, {@code CompileInvocation} exposed none of them, and nothing
     * downstream ever asked — so the parse always used the release of the JDK it happened to run on. See
     * {@code InputConfiguration.sourceRelease()} for what that costs when the two differ.
     */
    default int effectiveRelease() {
        int r = release();
        return r > 0 ? r : sourceRelease();
    }

    /**
     * The modules this invocation resolves against beyond the default root set ({@code --add-modules});
     * empty when it passed none. Recorded per invocation because it belongs to ONE source set: OpenSearch
     * passes {@code --add-modules jdk.incubator.vector} on 1 of its 47 javac lines, and the other 46 must not
     * have it — an incubator module made visible to a set whose build never had it is a parse that accepts
     * source the build would reject.
     */
    default List<String> addModules() {
        return List.of();
    }

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
