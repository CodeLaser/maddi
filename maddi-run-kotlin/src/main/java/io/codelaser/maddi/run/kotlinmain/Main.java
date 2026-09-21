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

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.run.config.report.ErrorReport;
import io.codelaser.maddi.run.config.report.ExitCode;
import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.run.kotlinmain.kotlinc.ParseMixedList;
import io.codelaser.maddi.run.openjdkmain.RunAnalyzer;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.DetectKotlinSources;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.codelaser.maddi.run.openjdkmain.Main.*;

/**
 * <b>The mixed Java+Kotlin CLI — one entry point, not a second tool.</b> It takes the <i>same</i> command line
 * as {@code bin/maddi} (the option surface is literally {@link io.codelaser.maddi.run.openjdkmain.Main#createOptions()},
 * not a second list that happens to agree), and routes on what the project turns out to hold:
 *
 * <ul>
 *   <li><b>No {@code .kt} file</b> — the run IS {@code maddi}: same {@link RunAnalyzer}, same behaviour, same
 *   exit codes. {@code bin/maddi-kotlin} is therefore a strict superset of {@code bin/maddi}, which is what
 *   lets a user with one mixed repository install one tool.</li>
 *   <li><b>Kotlin present</b> — the mixed pipeline ({@link RunMixedPrepAnalyzer}): both front ends share one
 *   core, so a cross-language reference resolves to a single type.</li>
 * </ul>
 *
 * <p>⛔ <b>An option the mixed pipeline does not honour is REFUSED by name</b> (exit
 * {@value ExitCode#UNSUPPORTED_OPTION}), never silently dropped — the same rule that made
 * {@code --skip-kotlin-sources} necessary on the Java side. Today that is persistence and everything built on
 * it: {@code --analysis-results-dir}, {@code --incremental-analysis}, {@code --analysis-steps rewire-tests},
 * and the analysis-hints compiler modes. Those wait on a Kotlin codec round trip, and a run that wrote an
 * empty result directory would look exactly like one that worked.
 *
 * <p>The JVM must be started with the openjdk {@code --add-exports jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED}
 * (the launcher and the test task inject them).
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static final int EXIT_OK = ExitCode.OK;

    /** What {@code --help} calls this tool: the launcher is {@code bin/maddi-kotlin} (see PUBLISHING.md). */
    public static final String PROGRAM_NAME = "maddi-kotlin";

    /** {@code --compile-log}: javac AND kotlinc invocations out of one log, linked by output identity. */
    public static final CompileLogParser MIXED_COMPILE_LOG =
            (log, jmods) -> new ParseMixedList().parse(log, jmods);

    public static void main(String[] args) {
        int exitValue = execute(args);
        if (exitValue != EXIT_OK) {
            LOGGER.error(ExitCode.message(exitValue));
            System.exit(exitValue);
        }
    }

    /**
     * The mixed CLI's command line. ⭐ It IS the Java CLI's — not a second list that happens to agree today.
     * {@code TestOneEntryPoint} reads it from here, so a future divergence fails a test rather than a user.
     */
    static Options cliOptions() {
        return createOptions();
    }

    public static int execute(String[] args) {
        try {
            CommandLineParser commandLineParser = new DefaultParser();
            Options options = cliOptions();
            CommandLine cmd = commandLineParser.parse(options, args);
            Configuration configuration = parseConfiguration(cmd, options, PROGRAM_NAME, MIXED_COMPILE_LOG);

            // terminal, as in the Java CLI: write the derived configuration and exit, no analysis. Combined
            // with --compile-log this is how a Kotlin corpus's checked-in inputConfiguration.json is produced.
            String writeInputConfiguration = cmd.getOptionValue(WRITE_INPUT_CONFIGURATION);
            if (writeInputConfiguration != null) {
                File file = new File(writeInputConfiguration);
                LOGGER.info("Writing input configuration to {} and exiting (no analysis)", file);
                JsonStreaming.objectMapper().writerWithDefaultPrettyPrinter()
                        .writeValue(file, configuration.inputConfiguration());
                return EXIT_OK;
            }

            InputConfiguration inputConfiguration = configuration.inputConfiguration();
            DetectKotlinSources kotlinSources = DetectKotlinSources.in(inputConfiguration);
            if (!kotlinSources.found()) {
                LOGGER.info("No Kotlin source file in {} source set(s); running the Java analyzer",
                        inputConfiguration.sourceSets().size());
                return runJavaAnalyzer(configuration);
            }
            // ⚠ --skip-kotlin-sources means the same thing on both CLIs: analyze the Java half and say so.
            // On `maddi` it lifts a refusal; here it asks for the Java pipeline over a project this tool CAN
            // read in full. Ignoring it because "this one does Kotlin" would be exactly the silent drop the
            // option exists to prevent — and it is the only way to get the flags the mixed pipeline refuses.
            if (configuration.generalConfiguration().skipKotlinSources()) {
                LOGGER.warn("{} Kotlin source file(s) in {} are NOT analyzed: {} was given. The findings cover"
                            + " the Java sources alone.", kotlinSources.fileCount(),
                        kotlinSources.sourceSetNames(), DetectKotlinSources.SKIP_OPTION);
                return runJavaAnalyzer(configuration);
            }
            LOGGER.info("{} Kotlin source file(s) in {}; running the mixed Java+Kotlin analysis",
                    kotlinSources.fileCount(), kotlinSources.sourceSetNames());
            return runMixed(configuration);
        } catch (ParseException parseException) {
            LOGGER.error("Parse exception: ", parseException);
            return ExitCode.INTERNAL_EXCEPTION;
        } catch (IOException ioException) {
            ErrorReport.report(null, ioException);
            return ExitCode.IO_EXCEPTION;
        } catch (RuntimeException runtimeException) {
            ErrorReport.report(null, runtimeException);
            return ExitCode.ANALYZER_ERROR;
        }
    }

    /** The Java-only route: byte-for-byte what {@code bin/maddi} does with the same arguments. */
    private static int runJavaAnalyzer(Configuration configuration) {
        RunAnalyzer runAnalyzer = new RunAnalyzer(configuration);
        runAnalyzer.run();
        if (!configuration.generalConfiguration().quiet()) {
            runAnalyzer.printSummaries();
        }
        return runAnalyzer.exitValue();
    }

    private static int runMixed(Configuration configuration) throws IOException {
        List<String> unsupported = unsupportedOptions(configuration);
        if (!unsupported.isEmpty()) {
            LOGGER.error("""
                    These options are not (yet) honoured for a project that contains Kotlin: {}.
                    They all depend on persisting an analysis result, which needs a Kotlin codec round trip \
                    (see docs/kotlin-gap-analysis-2026-09-21.md §8.6). Refusing rather than running them as \
                    no-ops: a run that wrote an empty result directory would look like one that worked. \
                    Drop the option, or analyze the Java half with `bin/maddi`.""", unsupported);
            return ExitCode.UNSUPPORTED_OPTION;
        }
        List<String> analysisSteps = configuration.generalConfiguration().analysisSteps();
        if (analysisSteps.size() == 1 && AS_NONE.equalsIgnoreCase(analysisSteps.getFirst())) {
            LOGGER.info("--{} {}: nothing to do", ANALYSIS_STEPS, AS_NONE);
            return EXIT_OK;
        }
        boolean modification = analysisSteps.contains(AS_MODIFICATION);
        RunMixedPrepAnalyzer.Options options = new RunMixedPrepAnalyzer.Options(modification,
                configuration.analysisHintsConfiguration() == null ? List.of()
                        : configuration.analysisHintsConfiguration().preloadAnalysisResultsDirs(),
                configuration.generalConfiguration().parallel(),
                configuration.generalConfiguration().warnNearMisses());
        RunMixedPrepAnalyzer.Summary summary = new RunMixedPrepAnalyzer()
                .go(configuration.inputConfiguration(), options);
        // the placeholder count belongs on the SAME line as the type counts: a run that reports what it
        // parsed without reporting what it could not read invites the reader to take the first for the whole
        LOGGER.info("Mixed {} complete: {} Kotlin + {} Java type(s), {} primary; analysis order size {};"
                    + " {} unreadable Kotlin construct(s)",
                modification ? AS_MODIFICATION : AS_PREP, summary.kotlinTypes(), summary.javaTypes(),
                summary.primaryTypes(), summary.analysisOrderSize(), summary.placeholders());
        // isolated elements are reported in full by the runner; the exit code must not call them a success
        if (summary.prepErrors() > 0) {
            LOGGER.error("{} element(s) were isolated by prep and not analyzed", summary.prepErrors());
            return ExitCode.ANALYZER_ERROR;
        }
        return EXIT_OK;
    }

    /**
     * The options the Java pipeline honours and the mixed one does not, by the name the user typed. Every one
     * of them ends at the same place — an analysis result that can be written and read back — which is why
     * they are listed here together rather than refused one at a time where they would be used.
     */
    static List<String> unsupportedOptions(Configuration configuration) {
        List<String> unsupported = new ArrayList<>();
        var general = configuration.generalConfiguration();
        String resultsDir = general.analysisResultsDir();
        if (resultsDir != null && !resultsDir.isBlank() && !AS_NONE.equalsIgnoreCase(resultsDir)) {
            unsupported.add("--" + ANALYSIS_RESULTS_DIR);
        }
        if (general.incrementalAnalysis()) unsupported.add("--" + INCREMENTAL_ANALYSIS);
        if (general.analysisSteps().contains(AS_REWIRE_TESTS)) {
            unsupported.add("--" + ANALYSIS_STEPS + " " + AS_REWIRE_TESTS);
        }
        var hints = configuration.analysisHintsConfiguration();
        if (hints != null) {
            if (hints.analysisResultsTargetDir() != null) unsupported.add("--" + ANALYSIS_RESULTS_TARGET_DIR);
            if (hints.updatedHintsDir() != null) unsupported.add("--" + UPDATED_HINTS_DIR);
        }
        return unsupported;
    }
}
