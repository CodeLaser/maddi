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

package io.codelaser.maddi.analysis.api;

import java.nio.file.Path;
import java.util.List;

/**
 * One analysis-hints compilation: the library, where its hints sources are, which package to restrict to, the
 * results to preload, and where to write results and (optionally) updated hints. The base-tier twin of
 * maddi-mod's {@code AnalysisHints}, field for field.
 */
public record HintsSpec(String libraryName,
                        Path hintsPath,
                        String packagePrefix,
                        List<String> preloadAnalysisResultsDirs,
                        Path analysisResultsDir,
                        Path updatedHintsPath) {
}
