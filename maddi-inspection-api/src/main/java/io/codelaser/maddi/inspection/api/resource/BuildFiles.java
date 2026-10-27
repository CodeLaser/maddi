/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 *                    at the root; build output directories are not searched. Each module-info.java at the top of
 *                    one of the configuration's source directories, where javac reads it, is a literal glob of its
 *                    own: never a root-wide {@code **}{@code /module-info.java}, which under a root holding more than
 *                    the analysed code (an audit's ~/git) would watch every other checkout
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

    /** Directories never searched: build output and tool state. */
    public static final List<String> EXCLUDED_DIRECTORIES = List.of("build", "target", ".gradle", ".git", ".idea",
            "node_modules", "out");

    /** The files Gradle reads to configure a build, besides those of included builds outside the root. */
    public static final List<String> GRADLE_GLOBS = List.of("settings.gradle", "settings.gradle.kts",
            "**/build.gradle", "**/build.gradle.kts", "gradle/*.versions.toml", "**/gradle.properties",
            "buildSrc/**", "gradle/wrapper/gradle-wrapper.properties");

    /** The files Maven reads to configure a build. */
    public static final List<String> MAVEN_GLOBS = List.of("**/pom.xml", ".mvn/**");

    public BuildFiles {
        globs = List.copyOf(globs);
        sha256 = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(sha256)); // sorted: a stable file
    }
}
