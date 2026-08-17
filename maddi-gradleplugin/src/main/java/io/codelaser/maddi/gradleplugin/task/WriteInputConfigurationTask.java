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
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Writes the project's {@link InputConfiguration} (extracted from the plugin-computed
 * {@link Configuration}) to a JSON file. Modern lazy-property task: configuration-cache compatible, holds no
 * {@code Project} at execution.
 */
@DisableCachingByDefault(because = "Trivial JSON re-serialization; cheaper to re-run than to cache and load.")
public abstract class WriteInputConfigurationTask extends DefaultTask {
    private static final Logger LOGGER = Logging.getLogger(WriteInputConfigurationTask.class);

    /** The serialized {@code run-config} {@link Configuration}; its input configuration is written out. */
    @Input
    public abstract Property<String> getConfigurationJson();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void run() {
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        File outputFile = getOutputFile().getAsFile().get();
        try {
            Configuration configuration = objectMapper.readerFor(Configuration.class)
                    .readValue(getConfigurationJson().get());
            InputConfiguration inputConfiguration = configuration.inputConfiguration();
            File parent = outputFile.getParentFile();
            if (parent != null && parent.mkdirs()) {
                LOGGER.debug("Created parent directories for output file {}", outputFile);
            }
            objectMapper.writerFor(InputConfigurationImpl.class)
                    .withDefaultPrettyPrinter()
                    .writeValue(outputFile, inputConfiguration);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Cannot write the input configuration", ioException);
        }
    }
}
