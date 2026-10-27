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

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A release stated by SOME compile invocations must not become the configuration's global release: the parse
 * applies the global to every set without its own, so it would reach the invocations that stated none — and those
 * compiled against their build's own JDK. Found configuring the jfocus/maddi workspace (2026-09-14): 4 of 90
 * invocations passed {@code --release 21}, 86 only {@code -source}, and all 86 were parsed at {@code --release=21}.
 * The shared-JDK half of the same fix is {@code TestSharedJdkRelease#aSilentSetCountsAsTheRunningJdk}.
 */
public class TestPartialReleaseIsNotGlobal {

    private static final String ROOT = "/checkout/ws";

    private record Invocation(String destination, List<String> sourcePath, List<String> classpath,
                              List<String> modulePath, int release, int sourceRelease) implements CompileInvocation {
        @Override
        public List<String> sourceFiles() {
            return List.of();
        }

        @Override
        public String encoding() {
            return null;
        }
    }

    private static Invocation gradle(String module, List<String> classpath, List<String> modulePath, int release,
                                     int sourceRelease) {
        return new Invocation(ROOT + "/" + module + "/build/classes/java/main",
                List.of(ROOT + "/" + module + "/src/main/java"), classpath, modulePath, release, sourceRelease);
    }

    private static InputConfiguration build(Invocation... invocations) {
        return CompileListToInputConfiguration.build(new CompileListToSourceSets(ROOT).compute(List.of(invocations)),
                List.of());
    }

    private static SourceSet set(InputConfiguration ic, String name) {
        return ic.sourceSets().stream().filter(s -> name.equals(s.name())).findFirst()
                .orElseThrow(() -> new AssertionError(name + " is nowhere"));
    }

    @DisplayName("CONTROL: when EVERY invocation states --release 21, the configuration does too")
    @Test
    public void aUnanimousReleaseIsGlobal() {
        InputConfiguration ic = build(gradle("a", List.of(), List.of(), 21, 0), gradle("b", List.of(), List.of(), 21, 0));
        assertEquals(21, ic.sourceRelease());
    }

    @DisplayName("a release stated by SOME invocations stays per set: it must not reach those that stated none")
    @Test
    public void aPartialReleaseIsNotGlobal() {
        InputConfiguration ic = build(
                gradle("stated", List.of(), List.of(), 21, 0),
                gradle("silent", List.of(), List.of(), 0, 25));
        assertEquals(0, ic.sourceRelease(),
                "'silent' compiled against its build's own JDK; a global 21 would parse it at --release=21");
        assertEquals(21, set(ic, "stated/main").sourceRelease(), "the stating set keeps its release per set");
        assertEquals(0, set(ic, "silent/main").sourceRelease(), "-source is the language level, not the API");
    }

    @DisplayName("CONTROL: no invocation states a release, no global release")
    @Test
    public void noReleaseNoGlobal() {
        InputConfiguration ic = build(gradle("a", List.of(), List.of(), 0, 25), gradle("b", List.of(), List.of(), 0, 17));
        assertEquals(0, ic.sourceRelease());
    }
}
