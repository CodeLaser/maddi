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

package org.e2immu.analyzer.run.config.report;

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
    public static final int ANALYSER_ERROR = 5;

    public static String message(int exitValue) {
        return switch (exitValue) {
            case OK -> "OK";
            case INTERNAL_EXCEPTION -> "Internal exception";
            case PARSER_ERROR -> "Parser error(s)";
            case INSPECTION_ERROR -> "Inspection error(s)";
            case IO_EXCEPTION -> "IO exception";
            case ANALYSER_ERROR -> "Analyser error(s)";
            default -> "Unknown exit code " + exitValue;
        };
    }
}
