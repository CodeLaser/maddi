/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.cst.api.element;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes a named collection of source files or a compiled library that the analyser can inspect.
 * <p>
 * A {@code SourceSet} is one of:
 * <ul>
 *   <li><b>Project sources</b> — Java files in a directory tree; {@link #parsedFromSource()} is {@code true}.</li>
 *   <li><b>External library</b> — a JAR or JMod on the classpath; {@link #externalLibrary()} is {@code true}.
 *       Sources may optionally be provided (from the JAR itself or a decompiler output) so that
 *       Annotated-API files can be generated.</li>
 *   <li><b>JDK module</b> — a JDK module treated as an external library with {@link #partOfJdk()} {@code true}.</li>
 * </ul>
 * <p>
 * Source sets form a dependency graph via {@link #dependencies()}: dependent sets are inspected
 * in dependency order so that types they export are resolved before the dependent set is parsed.
 */
public interface SourceSet {

    /** Returns the character encoding used to read source files (defaults to UTF-8). */
    default Charset sourceEncoding() {
        return StandardCharsets.UTF_8;
    }

    /** Returns the logical name of this source set (e.g. {@code "main"}, {@code "test"}, or a library name). */
    String name();

    /**
     * If this source set represents sources, this path points to a directory structure that contain the sources.
     * In the case of Java, the directory structure must be compatible with the package of the compilation unit.
     * <p>
     * If this source set represents an external library, this path is either <code>null</code>, or points to
     * the directory where sources have been expanded or computed using a decompiler.
     * The location of the library (jar) is given by the <code>uri()</code>.
     * <p>
     *
     * @return a path representing a directory containing source files
     */
    default List<Path> sourceDirectories() {
        return List.of();
    }

    /**
     * Valid URI with a non-null scheme.
     * <ul>
     *     <li>source directory: 'file' scheme, logically identical to <code>sourceDirectory()</code></li>
     *     <li>external jar on file system: a file with the 'file' scheme, name ends in .jar</li>
     *     <li>a test-protocol entry in a fqn->source map: 'test-protocol' scheme </li>
     * </ul>
     *
     * <p>
     * Some schemes have a special meaning, and will be intercepted: "test-protocol", "jar-on-classpath", "jmod".
     *
     * @return this source set's URI. Cannot be null.
     */
    URI uri();

    /** Returns {@code true} if this source set provides Java source files (not a pre-compiled library). */
    default boolean parsedFromSource() {
        return !externalLibrary();
    }

    /** Returns {@code true} if this source set contains test sources. */
    default boolean test() {
        return false;
    }

    /**
     * Returns {@code true} if this is an external library that is only needed at runtime,
     * not at compile time (e.g. a runtime-only dependency in Gradle).
     */
    default boolean runtimeOnly() {
        return false;
    }

    /** Returns {@code true} if this source set is a library (external or internal). */
    default boolean library() {
        return false;
    }

    /** Returns {@code true} if this source set is an external library (JAR, JMod, or decompiled). */
    default boolean externalLibrary() {
        return false;
    }

    /** Returns {@code true} if this source set is part of the JDK. Implies {@link #externalLibrary()}. */
    default boolean partOfJdk() {
        return false;
    }

    /** Returns {@code true} if this source set represents a Java module ({@code module-info.java}). */
    default boolean isModule() {
        return false;
    }

    /**
     * Returns a set of package name prefixes to which inspection of this source set is restricted.
     * An empty set means no restriction (all packages are inspected).
     *
     * @deprecated <b>Legacy; do not use in new code, and prefer removing it from existing
     * configurations.</b> Package restriction dates from the hand-written parser, which could not yet
     * process arbitrary code and needed a way to be pointed at the part it could manage. The current front
     * ends have no such limitation, so the option now buys nothing but trouble.
     * <p>
     * Concretely, it is <em>fatal on any modular project</em>. A restriction makes
     * {@code JavaInspectorImpl} place the source roots on javac's {@code SOURCE_PATH}; javac finds the
     * {@code module-info.java} there and compiles it implicitly, even though {@code ParseOptions.ignoreModule}
     * had deliberately excluded it from the compilation units. The compilation becomes a named module,
     * everything on the class path lands in the unnamed module, and every cross-module reference fails with
     * "package X does not exist". See {@code dogfood/README.md}.
     */
    @Deprecated
    default Set<String> restrictToPackages() {
        return Set.of();
    }

    /**
     * Returns the source sets that must be resolved before this one can be inspected or compiled.
     * The order matters: on a class-by-class basis, earlier entries take priority over later ones.
     */
    default List<SourceSet> dependencies() {
        return List.of();
    }

    /**
     * Used to determine whether the source of any of the types in this source set has changed.
     * Throws an error when not yet set.
     * <p>
     * The value may be computed from the sources in the <code>path</code>, or from any jar file that is their origin.
     */
    default FingerPrint fingerPrintOrNull() {
        return null;
    }

    /**
     * can be set only once.
     *
     * @param fingerPrint the source fingerprint
     */
    default void setFingerPrint(FingerPrint fingerPrint) {
    }

    /**
     * Used to determine whether the analysis of the whole source set has changed.
     * If so, dependent source sets may have to be re-analyzed.
     * This is typically implemented using a setOnce object since the source set has to be in place before
     * the analysis can take place.
     */
    default FingerPrint analysisFingerPrintOrNull() {
        return null;
    }

    default void setAnalysisFingerPrint(FingerPrint fingerPrint) {
    }

    /**
     * Returns {@code true} if a type in the given package and with the given name should be
     * accepted for inspection from this source set (respects {@link #restrictToPackages()}).
     */
    default boolean acceptSource(String packageName, String typeName) {
        return false;
    }

    /** Returns a copy of this source set with the given dependency list. */
    default SourceSet withDependencies(List<SourceSet> dependencies) {
        throw new UnsupportedOperationException();
    }

    /** Returns a copy of this source set with updated source directories and URI. */
    default SourceSet withSourceDirectoriesUri(List<Path> sourceDirectories, URI uri) {
        throw new UnsupportedOperationException();
    }

    /** Returns a copy of this source set with updated source directories. */
    default SourceSet withSourceDirectories(List<Path> sourceDirectories) {
        throw new UnsupportedOperationException();
    }

    /**
     * Pre-computes the transitive dependency priority order used by {@link #priorityDependencies()}.
     * Should be called once after the full dependency graph is assembled.
     */
    default void computePriorityDependencies() {
    }

    /**
     * Returns a map from dependency source sets to their priority level (lower = higher priority),
     * as computed by {@link #computePriorityDependencies()}.
     */
    default Map<SourceSet, Integer> priorityDependencies() {
        return Map.of();
    }
}
