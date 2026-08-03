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

package org.e2immu.analyzer.run.kotlinmain;

import org.e2immu.analyzer.run.config.report.ErrorReport;
import org.e2immu.analyzer.run.config.report.ExitCode;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.analyzer.run.kotlinmain.kotlinc.ParseMixedList;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI for the prep-only mixed Java+Kotlin analysis (the Kotlin counterpart of {@code run-openjdk}'s {@code Main}).
 * It obtains an {@link InputConfiguration} either from a mixed build/compile log ({@code --compile-log}, parsed by
 * {@link ParseMixedList} — the javac + kotlinc invocations link into one configuration) or from a serialized
 * configuration ({@code --input-configuration}), then runs {@link RunMixedPrepAnalyzer} and prints its summary.
 * <p>
 * {@code --write-input-configuration <file>} is terminal — write the derived configuration and exit, no analysis —
 * exactly as in {@code run-openjdk}. Combined with {@code --compile-log} it is how a Kotlin corpus's checked-in
 * {@code inputConfiguration.json} is produced, which is the whole point of having it here too.
 * <p>
 * The JVM must be started with the openjdk {@code --add-exports jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED}
 * (the {@code application} run task and the test task inject them).
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static final int EXIT_OK = ExitCode.OK;

    static final String COMPILE_LOG = "--compile-log";
    static final String INPUT_CONFIGURATION = "--input-configuration";
    static final String EXTRA_JMOD = "--extra-jmod";
    /** Terminal, as in {@code run-openjdk}'s Main: write the derived configuration and exit, no analysis. */
    static final String WRITE_INPUT_CONFIGURATION = "--write-input-configuration";
    /** {@value #AS_PREP} (default) or {@value #AS_MODIFICATION}, as in {@code run-openjdk}'s Main. */
    static final String ANALYSIS_STEPS = "--analysis-steps";
    static final String AS_PREP = "prep";
    static final String AS_MODIFICATION = "modification";
    /** Repeatable, as in {@code run-openjdk}: directories of pre-analyzed library annotations (the AAPI). */
    static final String PRELOAD_ANALYSIS_RESULTS_DIRS = "--preload-analysis-results-dirs";

    public static void main(String[] args) {
        int exitValue = execute(args);
        if (exitValue != EXIT_OK) {
            System.exit(exitValue);
        }
    }

    static int execute(String[] args) {
        String compileLog = null;
        String inputConfigurationFile = null;
        String writeInputConfiguration = null;
        String analysisSteps = AS_PREP;
        List<String> extraJmods = new ArrayList<>();
        List<String> analysisResultsDirs = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case COMPILE_LOG -> compileLog = value(args, ++i);
                case INPUT_CONFIGURATION -> inputConfigurationFile = value(args, ++i);
                case EXTRA_JMOD -> extraJmods.add(value(args, ++i));
                case WRITE_INPUT_CONFIGURATION -> writeInputConfiguration = value(args, ++i);
                case ANALYSIS_STEPS -> analysisSteps = value(args, ++i);
                case PRELOAD_ANALYSIS_RESULTS_DIRS -> analysisResultsDirs.add(value(args, ++i));
                default -> {
                    LOGGER.error("Unknown argument '{}'. Use {} <file> or {} <file> [{} <module>]... [{} <file>]",
                            args[i], COMPILE_LOG, INPUT_CONFIGURATION, EXTRA_JMOD, WRITE_INPUT_CONFIGURATION);
                    return ExitCode.INTERNAL_EXCEPTION;
                }
            }
        }
        try {
            InputConfiguration inputConfiguration;
            if (inputConfigurationFile != null) {
                LOGGER.info("Reading input configuration from {}", inputConfigurationFile);
                inputConfiguration = JsonStreaming.objectMapper()
                        .readValue(new File(inputConfigurationFile), InputConfigurationImpl.class);
            } else if (compileLog != null) {
                LOGGER.info("Deriving input configuration from mixed compile log {} (extra jmods {})",
                        compileLog, extraJmods);
                inputConfiguration = new ParseMixedList().parse(Path.of(compileLog), extraJmods);
            } else {
                LOGGER.error("Provide either {} <file> or {} <file>", COMPILE_LOG, INPUT_CONFIGURATION);
                return ExitCode.INTERNAL_EXCEPTION;
            }
            if (writeInputConfiguration != null) {
                File file = new File(writeInputConfiguration);
                LOGGER.info("Writing input configuration to {} and exiting (no analysis)", file);
                JsonStreaming.objectMapper().writerWithDefaultPrettyPrinter().writeValue(file, inputConfiguration);
                return EXIT_OK;
            }
            if (!AS_PREP.equals(analysisSteps) && !AS_MODIFICATION.equals(analysisSteps)) {
                LOGGER.error("{} must be '{}' or '{}', not '{}'", ANALYSIS_STEPS, AS_PREP, AS_MODIFICATION,
                        analysisSteps);
                return ExitCode.INTERNAL_EXCEPTION;
            }
            boolean modification = AS_MODIFICATION.equals(analysisSteps);
            RunMixedPrepAnalyzer.Summary summary = new RunMixedPrepAnalyzer()
                    .go(inputConfiguration, modification, analysisResultsDirs);
            LOGGER.info("Mixed {} complete: {} Kotlin + {} Java type(s), {} primary; analysis order size {}",
                    analysisSteps, summary.kotlinTypes(), summary.javaTypes(), summary.primaryTypes(),
                    summary.analysisOrderSize());
            // isolated elements are reported in full by the runner; the exit code must not call them a success
            if (summary.prepErrors() > 0) {
                LOGGER.error("{} element(s) were isolated by prep and not analyzed", summary.prepErrors());
                return ExitCode.ANALYSER_ERROR;
            }
            return EXIT_OK;
        } catch (IOException ioException) {
            ErrorReport.report(null, ioException);
            return ExitCode.IO_EXCEPTION;
        } catch (RuntimeException runtimeException) {
            ErrorReport.report(null, runtimeException);
            return ExitCode.ANALYSER_ERROR;
        }
    }

    private static String value(String[] args, int i) {
        if (i >= args.length) throw new IllegalArgumentException("Missing value after " + args[i - 1]);
        return args[i];
    }
}
