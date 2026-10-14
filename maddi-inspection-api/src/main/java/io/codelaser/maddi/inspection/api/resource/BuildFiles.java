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

package io.codelaser.maddi.inspection.api.resource;

import java.util.List;
import java.util.Map;

/**
 * The build files that shaped an input configuration, hashed when the configuration was captured: a server compares
 * them with the tree it analyses to tell that the configuration has gone stale (an edit to a build file after the
 * capture, as de6a2a011 moving maddi-cst-print to implementation, which a stale capture silently missed: maddi#133).
 * Only the producer knows which files those are, so it declares them; written as the {@code "buildFiles"} key of the
 * input configuration's JSON. A configuration without the key reads as {@link #NONE}: staleness cannot be checked.
 *
 * @param declaredBy  {@link #GRADLE_PLUGIN}, {@link #MAVEN_PLUGIN}, {@link #COMPILE_LOG} or {@link #NONE}
 * @param root        the absolute directory the globs and the paths are relative to, normally the working directory
 * @param globs       {@code '**'}/{@code '*'} patterns relative to root; {@code **}{@code /x} also matches {@code x}
 *                    at the root. Always contains {@link #MODULE_INFO}; build output directories are not searched
 * @param sha256      every file the globs matched, and every file the producer named, by path relative to root
 *                    ({@code ../} for an included build outside it), to its hex SHA-256 at capture time
 * @param capturedAt  ISO-8601 time the hashes were taken
 */
public record BuildFiles(String declaredBy, String root, List<String> globs, Map<String, String> sha256,
                         String capturedAt) {

    public static final String GRADLE_PLUGIN = "gradle-plugin";
    public static final String MAVEN_PLUGIN = "maven-plugin";
    public static final String COMPILE_LOG = "compile-log";
    public static final String NONE = "none";

    /** Shapes module resolution whatever the build system: always watched. */
    public static final String MODULE_INFO = "**/module-info.java";

    /** Directories never searched: build output and tool state. */
    public static final List<String> EXCLUDED_DIRECTORIES = List.of("build", "target", ".gradle", ".git", ".idea",
            "node_modules", "out");

    /** The files Gradle reads to configure a build, besides those of included builds outside the root. */
    public static final List<String> GRADLE_GLOBS = List.of("settings.gradle", "settings.gradle.kts",
            "**/build.gradle", "**/build.gradle.kts", "gradle/*.versions.toml", "**/gradle.properties",
            "buildSrc/**", "gradle/wrapper/gradle-wrapper.properties", MODULE_INFO);

    /** The files Maven reads to configure a build. */
    public static final List<String> MAVEN_GLOBS = List.of("**/pom.xml", ".mvn/**", MODULE_INFO);

    public BuildFiles {
        globs = List.copyOf(globs);
        sha256 = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(sha256)); // sorted: a stable file
    }
}
