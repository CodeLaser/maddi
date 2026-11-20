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
package io.codelaser.maddi.run.kotlinmain;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.run.config.report.ErrorReport;
import io.codelaser.maddi.run.config.report.ExitCode;
import io.codelaser.maddi.run.config.util.JsonStreaming;

/**
 * The entry point a build plugin forks: one argument, a file holding a whole serialized {@link Configuration} --
 * general options, analysis hints and input, as the plugin computed them -- analyzed exactly as {@code bin/maddi-kotlin}
 * analyzes the configuration it parses from its command line ({@link Main#run}). The command line's
 * {@code --input-configuration} carries the input part alone, so a plugin would otherwise have to turn its options
 * back into flags.
 * <p>
 * Run from the {@code maddi-kotlin} distribution's {@code lib/}, with {@code -Dmaddi.k2.home} at its {@code lib-k2/}.
 */
public final class RunConfigurationFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunConfigurationFile.class);

    private RunConfigurationFile() {
    }

    public static void main(String[] args) {
        int exitValue = execute(args);
        if (exitValue != ExitCode.OK) LOGGER.error(ExitCode.message(exitValue));
        // always: the Kotlin front end leaves a non-daemon thread behind (see Main.main)
        System.exit(exitValue);
    }

    static int execute(String[] args) {
        if (args.length != 1) {
            LOGGER.error("Usage: RunConfigurationFile <configuration.json>");
            return ExitCode.INTERNAL_EXCEPTION;
        }
        Configuration configuration;
        try {
            configuration = JsonStreaming.objectMapper().readerFor(Configuration.class).readValue(new File(args[0]));
        } catch (IOException ioException) {
            ErrorReport.report(null, ioException);
            return ExitCode.IO_EXCEPTION;
        }
        return Main.run(configuration);
    }
}
