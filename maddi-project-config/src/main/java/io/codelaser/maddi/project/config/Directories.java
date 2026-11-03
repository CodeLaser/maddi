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

package io.codelaser.maddi.project.config;

import java.util.List;
import java.nio.file.Path;

/**
 * A project named by hand, for a build no capture fits: its main sources and class path, and optionally test sources
 * that see the main ones. Relative paths are taken against {@code workingDirectory}.
 *
 * @param outputDirectory the main sources' compiled classes, if any; the configuration does not record it (a caller
 *                        that builds its own source sets, as maddi-mcp does for its daemon, can)
 * @param sourceRelease   0: unknown
 * @param sdkHome         the JDK to resolve JDK types against; null: the analysis's own
 * @param jdkModules      the JDK modules the sources may use ({@code java.base}, ...)
 * @param sourceEncoding  null: UTF-8
 */
public record Directories(Path workingDirectory, List<String> sourceDirectories, List<String> testSourceDirectories,
                          List<String> classPath, List<String> testClassPath, String outputDirectory,
                          int sourceRelease, String sdkHome, List<String> jdkModules, String sourceEncoding) {

    public Directories {
        sourceDirectories = List.copyOf(sourceDirectories);
        testSourceDirectories = testSourceDirectories == null ? List.of() : List.copyOf(testSourceDirectories);
        classPath = classPath == null ? List.of() : List.copyOf(classPath);
        testClassPath = testClassPath == null ? List.of() : List.copyOf(testClassPath);
        jdkModules = jdkModules == null ? List.of() : List.copyOf(jdkModules);
        if (sourceDirectories.isEmpty()) throw new IllegalArgumentException("no source directories");
    }
}
