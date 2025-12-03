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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InputConfiguration {

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
