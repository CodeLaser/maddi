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

package org.e2immu.analyzer.run.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfiguration;
import org.e2immu.analyzer.aapi.parser.AnnotatedAPIConfigurationImpl;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.analyzer.run.config.GeneralConfiguration;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.cst.impl.runtime.LanguageConfigurationImpl;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static final int EXIT_OK = 0;
    public static final int EXIT_INTERNAL_EXCEPTION = 1;
    public static final int EXIT_PARSER_ERROR = 2;
    public static final int EXIT_INSPECTION_ERROR = 3;
    public static final int EXIT_IO_EXCEPTION = 4;
    public static final int EXIT_ANALYSER_ERROR = 5; // analyser found errors

    public static final String HELP = "help";

    public static final String JRE = "jre";
    public static final String QUIET = "quiet";
    public static final String PARALLEL = "parallel";
    public static final String SOURCE_ENCODING = "source-encoding";
    public static final String INCREMENTAL_ANALYSIS = "incremental-analysis";
    public static final String ANALYSIS_STEPS = "analysis-steps";

    public static final String AS_NONE = "none";
    public static final String AS_PREP = "prep";
    public static final String AS_MODIFICATION = "modification";
    public static final String AS_REWIRE_TESTS = "rewire-tests";

    public static final String ANALYSIS_RESULTS_DIR = "analysis-results-dir";
    public static final String DEBUG = "debug";

    public static final String INPUT_CONFIGURATION = "input-configuration";

    public static final String SOURCE = "source";
    public static final String TEST_SOURCE = "test-source";
    public static final String SOURCE_PACKAGES = "source-packages";
    public static final String TEST_SOURCE_PACKAGES = "test-source-packages";

    public static final String CLASSPATH = "classpath"; // ~ compileClassPath in Gradle
    public static final String RUNTIME_CLASSPATH = "runtime-classpath";
    public static final String TEST_CLASSPATH = "test-classpath";
    public static final String TESTS_RUNTIME_CLASSPATH = "test-runtime-classpath";
    public static final String EXCLUDE_FROM_CLASSPATH = "exclude-from-classpath";

    public static final String DEPENDENCIES = "dependencies";

    // use case 1
    public static final String ANALYZED_ANNOTATED_API_DIRS = "analyzed-annotated-api-dirs";
    // use case 2
    public static final String ANALYZED_ANNOTATED_API_TARGET_DIR = "analyzed-annotated-api-target-dir";
    // use case 3
    public static final String ANNOTATED_API_TARGET_DIR = "annotated-api-target-dir";
    public static final String ANNOTATED_API_TARGET_PACKAGE = "annotated-api-target-package";
    public static final String ANNOTATED_API_PACKAGES = "annotated-api-packages";

    public static final String COMMA = ",";

    public static String exitMessage(int exitValue) {
        return switch (exitValue) {
            case EXIT_OK -> "OK";
            case EXIT_INTERNAL_EXCEPTION -> "Internal exception";
            case EXIT_PARSER_ERROR -> "Parser error(s)";
            case EXIT_INSPECTION_ERROR -> "Inspection error(s)";
            case EXIT_IO_EXCEPTION -> "IO exception";
            case EXIT_ANALYSER_ERROR -> "Analyser error(s)";
            default -> throw new UnsupportedOperationException("don't know value " + exitValue);
        };
    }

    public static void main(String[] args) {
        try {
            int exitValue = execute(args);
            if (exitValue != EXIT_OK) {
                LOGGER.error(exitMessage(exitValue));
                System.exit(exitValue);
            }
        } catch (ParseException parseException) {
            LOGGER.error("Parse exception: ", parseException);
            System.exit(EXIT_INTERNAL_EXCEPTION);
        } catch (IOException ioException) {
            LOGGER.error("IOException: ", ioException);
            System.exit(EXIT_IO_EXCEPTION);
        }
    }

    private static int execute(String[] args) throws ParseException, IOException {
        CommandLineParser commandLineParser = new DefaultParser();
        Options options = createOptions();
        CommandLine cmd = commandLineParser.parse(options, args);
        Configuration configuration = parseConfiguration(cmd, options);

        // the following will be output if the CONFIGURATION logger is active!
        LOGGER.debug("Configuration:\n{}", configuration);
        RunAnalyzer runAnalyser = new RunAnalyzer(configuration);
        runAnalyser.run();
        if (!configuration.generalConfiguration().quiet()) {
            runAnalyser.printSummaries();
        }
        return runAnalyser.exitValue();
    }


    private static Options createOptions() {
        Options options = new Options();
        options.addOption("h", HELP, false, "Print help.");
        addGeneralConfigurationOptions(options);
        addInputConfigurationOptions(options);
        addAnnotatedAPIConfigurationOptions(options);
        return options;
    }

    private static Configuration parseConfiguration(CommandLine cmd, Options options) throws IOException {
        if (cmd.hasOption(HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.setWidth(128);
            formatter.printHelp("e2immu-analyser", options);
            System.exit(EXIT_OK);
        }
        Configuration.Builder builder = new Configuration.Builder();

        GeneralConfiguration generalConfiguration = parseGeneralConfiguration(cmd);
        builder.setGeneralConfiguration(generalConfiguration);

        InputConfiguration inputConfiguration = parseInputConfiguration(cmd);
        builder.setInputConfiguration(inputConfiguration);

        AnnotatedAPIConfiguration annotatedAPIConfiguration = parseAnnotatedAPIConfiguration(cmd);
        builder.setAnnotatedAPIConfiguration(annotatedAPIConfiguration);

        builder.setLanguageConfiguration(new LanguageConfigurationImpl(true));

        return builder.build();
    }

    public static Configuration fromPropertyMap(Map<String, String> kvMap) {
        Configuration.Builder builder = new Configuration.Builder();

        GeneralConfiguration generalConfiguration = generalConfiguration(kvMap);
        builder.setGeneralConfiguration(generalConfiguration);
        builder.setInputConfiguration(inputConfiguration(kvMap));
        builder.setAnnotatedAPIConfiguration(annotatedAPIConfiguration(kvMap));

        builder.setLanguageConfiguration(new LanguageConfigurationImpl(true));

        return builder.build();
    }

    /* ******* general configuration ******** */

    private static void addGeneralConfigurationOptions(Options options) {
        options.addOption(Option.builder().longOpt(ANALYSIS_STEPS).hasArg().argName("STEPS")
                .desc("""
                        Provide an alternative list of analysis steps, separated by comma, or with multiple values \
                        of this property. Choose from:
                         - 'none': must be present as the only one.
                         - 'prep' runs the prep-analyzer:
                              hidden content types, hidden content selector,
                              variable data, part of construction, final fields,
                              analysis order
                         - 'analysis-order' implies prep, and prints out the order
                         - 'modification' runs
                              linked variables, static assignments,
                              fluent, identity,
                              modification, independence, immutability.
                        """).build());
        options.addOption(Option.builder().longOpt(ANALYSIS_RESULTS_DIR).hasArg().argName("DIR")
                .desc("""
                        Directory to write results of analyzer.
                        """).build());
        options.addOption(Option.builder("d").longOpt(DEBUG).hasArg().argName("TARGETS")
                .desc("""
                        Debug targets. Choose from: X Y Z
                        """).build());
        options.addOption("i", INCREMENTAL_ANALYSIS, false, "Incremental analysis mode.");
        options.addOption("p", PARALLEL, false, "Parallelize as much as possible.");
        options.addOption("q", QUIET, false, "Silent mode. Do not write warnings, errors, etc. to stdout.");
    }

    public static GeneralConfiguration generalConfiguration(Map<String, String> kvMap) {
        GeneralConfiguration.Builder builder = new GeneralConfiguration.Builder();
        setBooleanProperty(kvMap, QUIET, builder::setQuiet);
        setBooleanProperty(kvMap, PARALLEL, builder::setParallel);
        setBooleanProperty(kvMap, INCREMENTAL_ANALYSIS, builder::setIncrementalAnalysis);
        setSplitStringProperty(kvMap, COMMA, DEBUG, builder::addDebugTargets);
        setSplitStringProperty(kvMap, COMMA, ANALYSIS_STEPS, builder::addAnalysisSteps);
        setStringProperty(kvMap, ANALYSIS_RESULTS_DIR, builder::setAnalysisResultsDir);
        return builder.build();
    }

    private static GeneralConfiguration parseGeneralConfiguration(CommandLine cmd) {
        GeneralConfiguration.Builder builder = new GeneralConfiguration.Builder();

        builder.setParallel(cmd.hasOption(PARALLEL));
        builder.setQuiet(cmd.hasOption(QUIET));
        builder.setIncrementalAnalysis(cmd.hasOption(INCREMENTAL_ANALYSIS));

        String[] analysisSteps = cmd.getOptionValues(ANALYSIS_STEPS);
        splitAndAdd(analysisSteps, COMMA, builder::addAnalysisSteps);

        String[] debugTargets = cmd.getOptionValues(DEBUG);
        splitAndAdd(debugTargets, COMMA, builder::addDebugTargets);

        builder.setAnalysisResultsDir(cmd.getOptionValue(ANALYSIS_RESULTS_DIR));

        return builder.build();
    }

    /* ******* input configuration ******** */

    private static void addInputConfigurationOptions(Options options) {
        options.addOption(Option.builder().longOpt(INPUT_CONFIGURATION).hasArg().argName("FILE")
                .desc("Directly specify the input configuration file").build());

        options.addOption(Option.builder().longOpt(JRE).hasArg().argName("DIR")
                .desc("Provide an alternative location for the Java Runtime Environment (JRE). " +
                      "If absent, the JRE from the analyser is used: '" + System.getProperty("java.home") + "'.").build());
        options.addOption(Option.builder().longOpt(SOURCE_ENCODING).hasArg().argName("ENCODING")
                .desc("Alternative source encoding. Default is UTF-8").build());

        options.addOption(Option.builder("s").longOpt(SOURCE).hasArg().argName("DIRS")
                .desc("Add a directory where the source files can be found. Use the Java path separator '" +
                      File.pathSeparator + "' to separate directories, " +
                      "or use this options multiple times. Default, when this option is absent, is '"
                      + InputConfigurationImpl.MAVEN_MAIN + "'.").build());

        options.addOption(Option.builder().longOpt(TEST_SOURCE).hasArg().argName("DIRS")
                .desc("Add a directory where the test source files can be found. Use the Java path separator '" +
                      File.pathSeparator + "' to separate directories, " +
                      "or use this options multiple times. Default, when this option is absent, is '"
                      + InputConfigurationImpl.MAVEN_TEST + "'.").build());

        options.addOption(Option.builder("cp").longOpt(CLASSPATH).hasArg().argName("CLASSPATH")
                .desc("Add classpath components, separated by the Java path separator '"
                      + File.pathSeparator + "'. Default, when this option is absent, is '"
                      + Arrays.toString(InputConfigurationImpl.DEFAULT_MODULES) + "'.").build());

        options.addOption(Option.builder().longOpt(SOURCE_PACKAGES).hasArg().argName("PACKAGES")
                .desc("Restrict the sources parsed to the paths" +
                      " specified in the argument. Use ',' to separate paths, or use this option multiple times." +
                      " Use a dot at the end of a package name to accept sub-packages.").build());

        options.addOption(Option.builder().longOpt(TEST_SOURCE_PACKAGES).hasArg().argName("PACKAGES")
                .desc("Restrict the test sources parsed to the paths" +
                      " specified in the argument. Use ',' to separate paths, or use this option multiple times." +
                      " Use a dot at the end of a package name to accept sub-packages.").build());

        options.addOption(Option.builder().longOpt(EXCLUDE_FROM_CLASSPATH).hasArg().argName("PATH")
                .desc("Jar names to be excluded from the classpath, to give priority to others").build());
    }

    private static InputConfiguration inputConfiguration(Map<String, String> kvMap) {
        String dependencies = kvMap.getOrDefault(DEPENDENCIES, "");
        String excludeFromClasspath = kvMap.getOrDefault(EXCLUDE_FROM_CLASSPATH, "");

        LOGGER.info("Building input configuration, dependencies are {}, exclude is {}", dependencies, excludeFromClasspath);

        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        setStringProperty(kvMap, JRE, builder::setAlternativeJREDirectory);
        setStringProperty(kvMap, SOURCE_ENCODING, builder::setSourceEncoding);

        setSplitStringProperty(kvMap, File.pathSeparator, SOURCE, builder::addSources);
        setSplitStringProperty(kvMap, File.pathSeparator, TEST_SOURCE, builder::addTestSources);
        setSplitStringProperty(kvMap, ",", SOURCE_PACKAGES, builder::addRestrictSourceToPackages);
        setSplitStringProperty(kvMap, ",", TEST_SOURCE_PACKAGES, builder::addRestrictTestSourceToPackages);

        setSplitStringProperty(kvMap, File.pathSeparator, CLASSPATH, builder::addClassPath);
        setSplitStringProperty(kvMap, File.pathSeparator, TEST_CLASSPATH, builder::addTestClassPath);
        setSplitStringProperty(kvMap, File.pathSeparator, RUNTIME_CLASSPATH, builder::addRuntimeClassPath);
        setSplitStringProperty(kvMap, File.pathSeparator, TESTS_RUNTIME_CLASSPATH, builder::addTestRuntimeClassPath);

        return builder.build();
    }

    private static InputConfiguration parseInputConfiguration(CommandLine cmd) throws IOException {
        String inputConfigurationFile = cmd.getOptionValue(INPUT_CONFIGURATION);
        if (inputConfigurationFile != null) {
            ObjectMapper objectMapper = JsonStreaming.objectMapper();
            File file = new File(inputConfigurationFile);
            LOGGER.info("Reading inputConfiguration from file {}", inputConfigurationFile);
            return objectMapper.readValue(file, InputConfigurationImpl.class);
        }
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();

        String alternativeJREDirectory = cmd.getOptionValue(JRE);
        builder.setAlternativeJREDirectory(alternativeJREDirectory);

        String sourceEncoding = cmd.getOptionValue(SOURCE_ENCODING);
        builder.setSourceEncoding(sourceEncoding);

        String[] sources = cmd.getOptionValues(SOURCE);
        splitAndAdd(sources, File.pathSeparator, builder::addSources);

        String[] testSources = cmd.getOptionValues(TEST_SOURCE);
        splitAndAdd(testSources, File.pathSeparator, builder::addTestSources);

        String[] classPaths = cmd.getOptionValues(CLASSPATH);
        splitAndAdd(classPaths, File.pathSeparator, builder::addClassPath);

        String[] restrictSourceToPackages = cmd.getOptionValues(SOURCE_PACKAGES);
        splitAndAdd(restrictSourceToPackages, COMMA, builder::addRestrictSourceToPackages);

        String[] restrictTestSourceToPackages = cmd.getOptionValues(TEST_SOURCE_PACKAGES);
        splitAndAdd(restrictTestSourceToPackages, COMMA, builder::addRestrictTestSourceToPackages);

        return builder.build();
    }

    /* ******* Annotated API configuration ******** */

    /*
    Use cases.
    1: normal reading of AAAPI files: specify ANALYZED_ANNOTATED_API directories
         = normal parsing, with AAPI files for libraries and partial results for sources.
         When in incremental mode, the AAAPI files containing source packages may be updated.
         The first directory is chosen to write new AAAPI files.
    2: compiling AAPI -> AAAPI: write packages ANNOTATED_API_PACKAGES from the source path
         in to WRITE_ANALYZED_ANNOTATED_API_DIR. The source path may contain AAPI classes, and regular classes.
         = running the shallow analyzer to prepare libraries for use by e2immu
    3: writing AAPI skeletons: use WRITE_ANNOTATED_API_DIR as the target directory for the skeletons.
        WRITE_ANNOTATED_API_PACKAGES contains the classes for which a skeleton will be generated.
        WRITE_ANNOTATED_API_TARGET_PACKAGE contains the package to write to.
         = running the composer


    Apply use case 3 to generate the AAPI files for a library.
    Edit them, then apply use case 2 to compile the edited AAPI files into AAAPI files for use in a project.
    */

    private static void addAnnotatedAPIConfigurationOptions(Options options) {
        options.addOption(Option.builder("s").longOpt(ANALYZED_ANNOTATED_API_DIRS).hasArg().argName("DIRS")
                .desc("Add a directory where the analyzed annotated API files can be found." +
                      " Use the Java path separator '" + File.pathSeparator + "' to separate directories, " +
                      "or use this options multiple times.").build());

        options.addOption(Option.builder().longOpt(ANALYZED_ANNOTATED_API_TARGET_DIR).hasArg().argName("DIR")
                .desc("Where to write analyzed Annotated API files.").build());

        options.addOption(Option.builder().longOpt(ANNOTATED_API_TARGET_DIR).hasArg().argName("DIR")
                .desc("Where to write Annotated API skeleton files, extracted from sources.").build());

        options.addOption(Option.builder().longOpt(ANNOTATED_API_TARGET_PACKAGE).hasArg().argName("PACKAGE")
                .desc("Which package to write the Annotated API skeleton files to").build());

        options.addOption(Option.builder().longOpt(ANNOTATED_API_PACKAGES).hasArg().argName("PACKAGES")
                .desc("Create AAPI skeletons from the following packages of the source.").build());

    }

    public static AnnotatedAPIConfiguration annotatedAPIConfiguration(Map<String, String> kvMap) {
        AnnotatedAPIConfigurationImpl.Builder builder = new AnnotatedAPIConfigurationImpl.Builder();

        setSplitStringProperty(kvMap, ",", ANALYZED_ANNOTATED_API_DIRS, builder::addAnalyzedAnnotatedApiDirs);

        setStringProperty(kvMap, ANALYZED_ANNOTATED_API_TARGET_DIR, builder::setAnalyzedAnnotatedApiTargetDir);
        setStringProperty(kvMap, ANNOTATED_API_TARGET_DIR, builder::setAnnotatedApiTargetDir);
        setStringProperty(kvMap, ANNOTATED_API_TARGET_PACKAGE, builder::setAnnotatedApiTargetPackage);

        setSplitStringProperty(kvMap, COMMA, ANNOTATED_API_PACKAGES, builder::addAnnotatedApiPackages);

        return builder.build();
    }

    private static AnnotatedAPIConfiguration parseAnnotatedAPIConfiguration(CommandLine cmd) {
        AnnotatedAPIConfigurationImpl.Builder builder = new AnnotatedAPIConfigurationImpl.Builder();

        String[] analyzedDirs = cmd.getOptionValues(ANALYZED_ANNOTATED_API_DIRS);
        splitAndAdd(analyzedDirs, ",", builder::addAnalyzedAnnotatedApiDirs);

        String writeAnalyzedDir = cmd.getOptionValue(ANALYZED_ANNOTATED_API_TARGET_DIR);
        builder.setAnalyzedAnnotatedApiTargetDir(writeAnalyzedDir);

        String writeDir = cmd.getOptionValue(ANNOTATED_API_TARGET_DIR);
        builder.setAnnotatedApiTargetDir(writeDir);

        String targetPackage = cmd.getOptionValue(ANNOTATED_API_TARGET_PACKAGE);
        builder.setAnnotatedApiTargetPackage(targetPackage);

        String[] writePackages = cmd.getOptionValues(ANNOTATED_API_PACKAGES);
        splitAndAdd(writePackages, COMMA, builder::addAnnotatedApiPackages);
        return builder.build();
    }

    // *************************************** */
    private static final String DO_NOT_SPLIT = "__DO_NOT_SPLIT__";

    private static void splitAndAdd(String[] strings, String separator, Consumer<String> adder) {
        if (strings != null) {
            for (String string : strings) {
                String s = string.replace("::", DO_NOT_SPLIT);
                String[] parts = s.split(separator);
                for (String part : parts) {
                    if (part != null && !part.trim().isEmpty()) {
                        adder.accept(part.replace(DO_NOT_SPLIT, ":"));
                    }
                }
            }
        }
    }

    static void setStringProperty(Map<String, String> properties, String key, Consumer<String> consumer) {
        String value = properties.get(key);
        if (value != null) {
            String trim = value.trim();
            if (!trim.isEmpty()) consumer.accept(trim);
        }
    }

    public static void setSplitStringProperty(Map<String, String> properties, String separator, String key, Consumer<String> consumer) {
        String value = properties.get(key);
        if (value != null) {
            String[] parts = value.split(separator);
            for (String part : parts) {
                if (part != null && !part.trim().isEmpty()) {
                    consumer.accept(part);
                }
            }
        }
    }

    public static void setBooleanProperty(Map<String, String> properties, String key, Consumer<Boolean> consumer) {
        String value = properties.get(key);
        if (value != null) {
            consumer.accept("true".equalsIgnoreCase(value.trim()));
        }
    }

}
