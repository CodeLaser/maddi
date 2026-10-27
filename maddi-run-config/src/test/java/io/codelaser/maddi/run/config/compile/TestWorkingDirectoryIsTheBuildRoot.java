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

package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ⛔⛔ <b>THE ANALYSED TREE MUST NOT BE FOREIGN TO ITSELF.</b> An {@code InputConfiguration}'s working
 * directory is what every relative path in it resolves against, and what a lever means by "inside the
 * project". Left at its default {@code "."} it is the JVM's own directory — so the corpus is outside the
 * project, and a lever that writes files says so.
 * <p>
 * ⚠ <b>MEASURED, ON ELASTICSEARCH, 2026-08-08.</b> {@code structure.writeModuleInfo} pointed at the corpus by
 * absolute path answered: <i>"Refusing to write outside the project:
 * …/es-phase3/libs/core resolves outside …/codelaser-refactor-graalpy"</i>. The generator this route replaced
 * set the working directory explicitly; this one knew the build root all along and never passed it on.
 * <p>
 * ⭐ The build root became a <b>configured</b> value rather than a derived one so that narrowing a parse would
 * not rename every source set it kept. This is the second thing that depends on knowing it, and the reason it
 * now travels on {@link CompileListToSourceSets.Result} rather than staying inside {@code compute}.
 */
public class TestWorkingDirectoryIsTheBuildRoot {

    private static final String ROOT = "/checkout/es";

    private record Invocation(String destination, List<String> sourcePath) implements CompileInvocation {
        @Override
        public List<String> classpath() {
            return List.of();
        }

        @Override
        public List<String> modulePath() {
            return null;
        }

        @Override
        public List<String> sourceFiles() {
            return List.of();
        }

        @Override
        public String encoding() {
            return null;
        }
    }

    private static InputConfiguration configuration(String buildRoot) {
        CompileListToSourceSets.Result result = new CompileListToSourceSets(buildRoot).compute(List.of(
                new Invocation(ROOT + "/libs/core/build/classes/java/main", List.of(ROOT + "/libs/core/src/main/java")),
                new Invocation(ROOT + "/server/build/classes/java/main", List.of(ROOT + "/server/src/main/java"))));
        return CompileListToInputConfiguration.build(result, List.of());
    }

    @DisplayName("the configured build root becomes the working directory")
    @Test
    public void theConfiguredBuildRootIsTheWorkingDirectory() {
        assertEquals(ROOT, configuration(ROOT).workingDirectory().toString());
    }

    /**
     * ⚠ CONTROL: with no build root configured, the derived one is still a real directory — the longest common
     * ancestor of the build units — and not {@code "."}. A caller who passes nothing must not silently get a
     * configuration rooted at wherever the JVM happens to be.
     */
    @DisplayName("CONTROL: with nothing configured, the DERIVED build root is used, never \".\"")
    @Test
    public void theDerivedBuildRootIsUsedWhenNothingIsConfigured() {
        assertEquals(ROOT, configuration(null).workingDirectory().toString());
    }
}
