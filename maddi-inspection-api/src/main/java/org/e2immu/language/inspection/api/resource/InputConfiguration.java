package org.e2immu.language.inspection.api.resource;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.SourceSet;

import java.nio.file.Path;
import java.util.List;

public interface InputConfiguration {

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

        // --- alternatives to addSourceSets

        @Fluent
        Builder addSources(String... sources);

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

    List<SourceSet> findMostLikelySourceSet(String name);

}
