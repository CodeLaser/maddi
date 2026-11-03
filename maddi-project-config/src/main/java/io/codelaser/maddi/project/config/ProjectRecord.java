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

import java.nio.file.Path;
import java.time.Instant;

/**
 * A configured project, as a {@link ProjectRecords} store keeps it.
 *
 * @param inputConfiguration where its configuration is: where a build tool wrote it, or in the store's
 *                           {@link ProjectRecords#workDirectory} when this library made it
 * @param regenerate         how to make it again; null when nothing can (directories named by hand)
 */
public record ProjectRecord(String name, ConfiguredBy configuredBy, Path inputConfiguration, Regenerate regenerate,
                            Instant configuredAt) {

    public ProjectRecord withConfiguredAt(Instant at) {
        return new ProjectRecord(name, configuredBy, inputConfiguration, regenerate, at);
    }

    public ProjectRecord withInputConfiguration(Path file, Instant at) {
        return new ProjectRecord(name, configuredBy, file, regenerate, at);
    }
}
