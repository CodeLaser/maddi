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

package io.codelaser.maddi.run.config.report;

/**
 * The analyzer's process exit codes, shared by every runner (openjdk, in-house/main, kotlin) so they report the
 * same code for the same failure. The values match the historical constants in each runner's {@code Main}.
 */
public final class ExitCode {
    private ExitCode() {
    }

    public static final int OK = 0;
    public static final int INTERNAL_EXCEPTION = 1;
    public static final int PARSER_ERROR = 2;
    public static final int INSPECTION_ERROR = 3;
    public static final int IO_EXCEPTION = 4;
    public static final int ANALYZER_ERROR = 5;
    /**
     * The run was handed Kotlin sources that the Java-only analyzer reads as nothing at all
     * ({@code DetectKotlinSources}); refusing beats reporting success over a tree it only partly read.
     */
    public static final int KOTLIN_SOURCES = 6;
    /**
     * An option was given that this pipeline does not honour. Refusing beats running it as a no-op: the
     * mixed Java+Kotlin runner cannot yet persist an analysis result, and a run that wrote an empty result
     * directory would look exactly like one that worked.
     */
    public static final int UNSUPPORTED_OPTION = 7;

    public static String message(int exitValue) {
        return switch (exitValue) {
            case OK -> "OK";
            case INTERNAL_EXCEPTION -> "Internal exception";
            case PARSER_ERROR -> "Parser error(s)";
            case INSPECTION_ERROR -> "Inspection error(s)";
            case IO_EXCEPTION -> "IO exception";
            case ANALYZER_ERROR -> "Analyzer error(s)";
            case KOTLIN_SOURCES -> "Kotlin source files present, which this analyzer cannot read";
            case UNSUPPORTED_OPTION -> "Option(s) not supported by this pipeline";
            default -> "Unknown exit code " + exitValue;
        };
    }
}
