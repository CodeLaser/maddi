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

package io.codelaser.maddi.inspection.resource;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Class-path entries for the third-party libraries whose analysis hints ship in the annotated-API archive
 * ({@code maddi-aapi-archive}'s {@code libs} jar: junit, slf4j/logback). Add them to an
 * {@link io.codelaser.maddi.inspection.api.resource.InputConfiguration.Builder#addClassPath(String...) classpath}
 * when a project references those libraries and you want their bundled hints to resolve against the real types --
 * e.g. {@code builder.addClassPath(AnalyzedApiClassPath.ALL)}.
 * <p>
 * Each entry uses the {@code jar-on-classpath:<package-path>} scheme, so the jar is located on the running JVM's
 * class path by a package it contains (no absolute paths). This is a single-purpose companion to the archive; it is
 * deliberately kept apart from the broader {@code ToolChain} grab-bag (junit/slf4j but also intellij, e2immu, ...),
 * which forwards to these constants for backward compatibility while it is phased out.
 * <p>
 * Absence is harmless: {@code LoadAnalysisResults} skips the hints for any library not on the classpath, so adding
 * these is only needed when you actively want the hints applied.
 */
public final class AnalyzedApiClassPath {
    private AnalyzedApiClassPath() {
    }

    private static final String JAR = "jar-on-classpath:";

    /** junit (jupiter api + its transitive api/commons/opentest4j), as covered by the archive's junit hints. */
    public static final String[] JUNIT = {
            JAR + "org/junit/jupiter/api",
            JAR + "org/apiguardian/api",
            JAR + "org/junit/platform/commons",
            JAR + "org/opentest4j"};

    /** slf4j and its logback binding, as covered by the archive's slf4j hints. */
    public static final String[] SLF4J_LOGBACK = {
            JAR + "org/slf4j/event",
            JAR + "ch/qos/logback/core",
            JAR + "ch/qos/logback/classic"};

    /** Every library the annotated-API archive covers, for one-line inclusion. */
    public static final String[] ALL =
            Stream.concat(Arrays.stream(JUNIT), Arrays.stream(SLF4J_LOGBACK)).toArray(String[]::new);
}
