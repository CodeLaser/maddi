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

package io.codelaser.maddi.run.openjdkmain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;

/**
 * CodeLaser/maddi#201: --test-source-packages governs the test sources (they had the main restriction), and
 * --test-classpath is a command-line option (it was read from the key-value configuration only).
 */
public class TestTestSourceOptions {

    private static InputConfiguration parse(String... args) throws Exception {
        CommandLine cmd = DefaultParser.builder().get().parse(Main.createOptions(), args);
        return Main.parseInputConfiguration(cmd, null);
    }

    private static SourceSet set(InputConfiguration ic, boolean test) {
        return ic.sourceSets().stream().filter(s -> s.test() == test).findFirst().orElseThrow();
    }

    @Test
    public void testPackagesGovernTheTestSources() throws Exception {
        InputConfiguration ic = parse("--source", "/tmp/src", "--source-packages", "a.main",
                "--test-source", "/tmp/test", "--test-source-packages", "a.test,a.helper");
        assertEquals(Set.of("a.main"), set(ic, false).restrictToPackages());
        assertEquals(Set.of("a.test", "a.helper"), set(ic, true).restrictToPackages());
    }

    @Test
    public void withoutTestPackagesTheMainRestrictionStillApplies() throws Exception {
        InputConfiguration ic = parse("--source", "/tmp/src", "--source-packages", "a.main",
                "--test-source", "/tmp/test");
        assertEquals(Set.of("a.main"), set(ic, true).restrictToPackages());
    }

    @Test
    public void testClasspathOption() throws Exception {
        InputConfiguration ic = parse("--source", "/tmp/src", "--test-source", "/tmp/test",
                "--test-classpath", "/tmp/junit.jar");
        List<String> testOnly = ic.classPathParts().stream().filter(SourceSet::test)
                .map(s -> s.uri().toString()).filter(u -> u.endsWith("junit.jar")).toList();
        assertEquals(1, testOnly.size(), ic.classPathParts().toString());
    }
}
