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
 * Possible situations.
 * <p>
 * There are quite a few external libraries that contain java sources in the .jar file.
 * If an external library has no sources, they can be generated using a decompiler. Either way, they should end up
 * in the sourceDirectory() for the modification analyzer to be able to produce Annotated-API files.
 */
public interface SourceSet {

    default Charset sourceEncoding() {
        return StandardCharsets.UTF_8;
    }

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

    default boolean parsedFromSource() {
        return !externalLibrary();
    }

    default boolean test() {
        return false;
    }

    /**
     * only relevant when external library is true
     *
     * @return if the source set is a library which is needed at runtime, but not at compile time.
     */
    default boolean runtimeOnly() {
        return false;
    }

    default boolean library() {
        return false;
    }

    default boolean externalLibrary() {
        return false;
    }

    default boolean partOfJdk() {
        return false;
    }

    default boolean isModule() {
        return false;
    }

    default Set<String> restrictToPackages() {
        return Set.of();
    }

    /* which sourceSets must be present for this source set to compile/run/resolve?
    The order is important: first come first served, on class-by-class basis
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

    // helper methods

    default boolean acceptSource(String packageName, String typeName) {
        return false;
    }

    default SourceSet withDependencies(List<SourceSet> dependencies) {
        throw new UnsupportedOperationException();
    }

    default SourceSet withSourceDirectoriesUri(List<Path> sourceDirectories, URI uri) {
        throw new UnsupportedOperationException();
    }

    default SourceSet withSourceDirectories(List<Path> sourceDirectories) {
        throw new UnsupportedOperationException();
    }

    default void computePriorityDependencies() {
    }

    default Map<SourceSet, Integer> priorityDependencies() {
        return Map.of();
    }
}
