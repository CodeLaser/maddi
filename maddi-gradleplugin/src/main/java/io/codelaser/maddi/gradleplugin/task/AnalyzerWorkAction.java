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

package org.e2immu.gradleplugin.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.e2immu.analyzer.run.config.Configuration;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.analyzer.run.main.Main;
import org.e2immu.analyzer.run.openjdkmain.RunAnalyzer;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * The actual analysis, run in a forked worker JVM (see {@link AnalyzerTask}) so the openjdk front-end's required
 * {@code --add-exports} for {@code com.sun.tools.javac.*} can be injected without the consuming build's daemon
 * needing any special configuration. Deserializes the {@link Configuration} and hands it to the openjdk-based
 * {@link RunAnalyzer}.
 */
public abstract class AnalyzerWorkAction implements WorkAction<AnalyzerWorkAction.Parameters> {

    public interface Parameters extends WorkParameters {
        Property<String> getConfigurationJson();
    }

    @Override
    public void execute() {
        String configurationJson = getParameters().getConfigurationJson().get();
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        Configuration configuration;
        try {
            configuration = objectMapper.readerFor(Configuration.class).readValue(configurationJson);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Cannot read the e2immu configuration", ioException);
        }
        RunAnalyzer runAnalyzer = new RunAnalyzer(configuration);
        runAnalyzer.run();
        int exitValue = runAnalyzer.exitValue();
        if (exitValue != Main.EXIT_OK) {
            throw new RuntimeException("e2immu analyzer failed (exit " + exitValue + "): " + Main.exitMessage(exitValue));
        }
    }
}
