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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Where configured projects are kept: maddi-mcp keeps them as JSON files; another tool keeps them its own way, and
 * whatever else it likes beside a record.
 */
public interface ProjectRecords {

    Optional<ProjectRecord> load(String name) throws IOException;

    /** Every stored project; one that cannot be read is skipped, and named in {@code problems}. */
    List<ProjectRecord> loadAll(List<String> problems);

    void save(ProjectRecord record) throws IOException;

    /**
     * Where this library writes for a project: the captured {@code compile.log} and {@code build.log}, and the
     * {@code inputConfiguration.json} it makes. Need not exist yet.
     */
    Path workDirectory(String name);
}
