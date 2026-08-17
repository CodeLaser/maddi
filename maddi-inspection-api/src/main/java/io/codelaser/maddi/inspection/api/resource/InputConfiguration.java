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

package org.e2immu.language.inspection.api.resource;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.SourceSet;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InputConfiguration {

    /**
     * A class path entry that is not a file but a <em>selector</em> into the running process' own class path:
     * either a package folder ({@code jar-on-classpath:org/e2immu/annotation}) or a jar file name
     * ({@code jar-on-classpath:slf4j-api-2.0.17.jar}). The front end resolves it to the real jar or exploded
     * directory at scan time — see {@code ClassSymbolScanner} — because the entry does not carry the physical
     * identity of the library, and its version is not known until we look.
     */
    String JAR_ON_CLASSPATH_PREFIX = "jar-on-classpath:";

    /**
     * The selector of a {@link #JAR_ON_CLASSPATH_PREFIX} class path entry, or null when {@code sourceSet} is
     * not one.
     * <p>
     * <b>Both the name and the URI are accepted, and that is the point of this method existing.</b> The two
     * ways of building such an entry disagreed about where the prefix goes:
     * {@code Builder.addClassPath(JAR_ON_CLASSPATH_PREFIX + "org/e2immu/annotation")} puts it on the name
     * (the URI follows), while {@code withSupportFromClasspath} puts it on the URI and names the set after
     * the caller's map key — the key being the point of that API. Every recognition site tested only the
     * name, so a set built the second way was never recognised, fell through to {@code Path.of(uri)} on an
     * opaque {@code jar-on-classpath:} URI, and died with
     * {@code FileSystemNotFoundException: Provider "jar-on-classpath" not installed}. That made
     * {@code withSupportFromClasspath} and {@code withE2ImmuSupportFromClasspath} unusable as written.
     * <p>
     * Asking the entry rather than its name settles it without taking the naming freedom away.
     */
    static String jarOnClasspathSelector(SourceSet sourceSet) {
        if (sourceSet == null) return null;
        String name = sourceSet.name();
        if (name != null && name.startsWith(JAR_ON_CLASSPATH_PREFIX)) {
            return name.substring(JAR_ON_CLASSPATH_PREFIX.length());
        }
        URI uri = sourceSet.uri();
        if (uri != null && "jar-on-classpath".equals(uri.getScheme())) {
            return uri.getSchemeSpecificPart();
        }
        return null;
    }

    default SourceSet javaBase() {
        return classPathParts().stream()
                .filter(set -> "java.base".equals(set.name()))
                .findFirst().orElseThrow();
    }

    /**
     * By default, this value is ".", representing the operating system's current working directory.
     * All relative paths in the source sets and class path parts are prefixed with this directory.
     *
     * @return the current working directory
     */
    Path workingDirectory();

    /**
     * At inspection level, the order of the source sets may be important, as packages/types may be ignored
     * when duplicates occur. This duplication needs to be resolved here; the concept of "hidden" or "inactive" types
     * in a source set does not exist at the CST level.
     * If the duplication occurs at package level (as an aggregate over the types), the <code>excludePackages()</code>
     * field in the source set may be used to store this information.
     */
    List<SourceSet> sourceSets();

    /**
     * At inspection level, the order of the class path parts may be important, as packages/types may be ignored
     * when duplicates occur. See <code>sourceSets()</code>.
     */
    List<SourceSet> classPathParts();

    default boolean containsLombok() {
        return classPathParts().stream().anyMatch(cp -> cp.externalLibrary() && cp.name().startsWith("lombok-"));
    }

    /**
     * this directory must be absolute. It is not prefixed by the <code>workingDirectory</code>.
     *
     * @return A path representing an absolute path towards the JRE that will be used to find the JMODs.
     */
    Path alternativeJREDirectory();

    /**
     * The Java release the corpus was <b>compiled against</b> ({@code javac --release N}), or a value {@code <= 0}
     * when it is not known — in which case the parse falls back to the release of the JDK it is running on.
     *
     * <p>⛔⛔ <b>THE RUNNING JDK IS NOT THE CORPUS'S PLATFORM, AND THE DIFFERENCE DELETES METHODS.</b> A corpus
     * built with {@code --release 17} may call APIs that a later JDK no longer has, and then the parse reports
     * <i>"cannot find symbol"</i> against source that its own build compiles without a murmur. Measured on
     * Apache Pulsar 5.0.0-M1, 2026-08-12: all 105 javac invocations pass {@code --release 17}; maddi ran on
     * JDK 26 and therefore parsed against 26, where {@code Thread.suspend()} and {@code Thread.resume()} have
     * been REMOVED. Three copies of bookkeeper's {@code ZooKeeperUtil} call them, javac stopped attributing,
     * and the units behind them were dropped.
     *
     * <p>⚠ It is mutually exclusive with {@link #alternativeJREDirectory()}: javac takes {@code --release} or
     * {@code --system}, never both. The alternative JRE wins where it is set, because it is the more specific
     * instruction.
     */
    int sourceRelease();

    interface Builder {

        @Fluent
        Builder setWorkingDirectory(String workingDirectory);

        @Fluent
        Builder addSourceSets(SourceSet... sourceSets);

        @Fluent
        Builder addClassPathParts(SourceSet... classPathParts);

        @Fluent
        Builder addSourceSets(Collection<SourceSet> sourceSets);

        @Fluent
        Builder addClassPathParts(Collection<SourceSet> classPathParts);

        // --- alternatives to addSourceSets

        @Fluent
        Builder addSources(String... sources);

        @Fluent
        Builder addSource(String sourceSetName, String sourceSetPath);

        @Fluent
        Builder addRestrictSourceToPackages(String... packages);

        @Fluent
        Builder addRestrictTestSourceToPackages(String... packages);

        // --- alternatives to addClassPathParts

        @Fluent
        Builder addClassPath(String... sources);

        @Fluent
        Builder addJmodToClassPath(String... sources);

        @Fluent
        Builder addRuntimeClassPath(String... sources);

        @Fluent
        Builder addTestClassPath(String... sources);

        @Fluent
        Builder addTestRuntimeClassPath(String... sources);

        @Fluent
        Builder addTestSources(String... sources);

        @Fluent
        Builder addTestSource(String sourceSetName, String sourceSetPath);

        // --- rest

        @Fluent
        Builder setAlternativeJREDirectory(String alternativeJREDirectory);

        /** The corpus's own {@code javac --release}; {@code <= 0} means "not known, use the running JDK". */
        @Fluent
        Builder setSourceRelease(int sourceRelease);

        @Fluent
        Builder setSourceEncoding(String sourceEncoding);

        InputConfiguration build();
    }

    // helper

    InputConfiguration withDefaultModules();

    InputConfiguration withE2ImmuSupportFromClasspath();

    InputConfiguration withSupportFromClasspath(Map<String, String> sourceSetNameToPackageDir);

    List<SourceSet> findMostLikelySourceSet(String name);

}
