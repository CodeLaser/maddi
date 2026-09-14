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

package io.codelaser.maddi.run.config.compile;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A NON-modular consumer gets a gradle sibling's jar ({@code build/libs/x-0.9.1.jar}) on {@code -classpath}. The
 * name rule ({@code computeModuleJars}) mapped such a jar back to its source set only on the module path, and the
 * maven path rule ({@code computePackagedJars}) never meets {@code build/libs}. Found configuring the jfocus/maddi
 * workspace itself (2026-09-14): {@code maddi-intellij} bound {@code maddi-ide-client-0.9.1.jar} as a library while
 * {@code maddi-ide-client} was a source set of the same parse. The maven form is {@link TestReactorJarOnClasspath}.
 */
public class TestGradleSiblingJarOnClasspath {

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

    private static String jar(String module) {
        return ROOT + "/" + module + "/build/libs/" + module + "-0.9.1.jar";
    }

    private static final String EXTERNAL_JAR = "/home/u/.gradle/caches/client-2.0.jar";

    private static SourceSet named(CompileListToSourceSets.Result r, String name) {
        return r.jSourceSets().stream().map(CompileListToSourceSets.JSourceSet::sourceSet)
                .filter(s -> name.equals(s.name())).findFirst()
                .orElseThrow(() -> new AssertionError(name + " is nowhere"));
    }

    private static List<String> dependencyNames(SourceSet set) {
        return set.dependencies().stream().map(SourceSet::name).sorted().toList();
    }

    private static List<String> jarNames(CompileListToSourceSets.Result r) {
        return r.jars().stream().map(SourceSet::name).sorted().toList();
    }

    @DisplayName("CONTROL: a MODULAR consumer has always resolved the sibling's jar, on the module path")
    @Test
    public void theModulePathFormResolves() {
        CompileListToSourceSets.Result r = new CompileListToSourceSets(ROOT).compute(List.of(
                gradle("ide-client", List.of(), List.of(), 0, 25),
                gradle("daemon", List.of(), List.of(jar("ide-client")), 0, 25)));
        assertEquals(List.of("ide-client/main"), dependencyNames(named(r, "daemon/main")));
    }

    @DisplayName("a NON-modular consumer's -classpath jar of a sibling becomes that sibling's source set")
    @Test
    public void theClassPathFormResolvesToo() {
        CompileListToSourceSets.Result r = new CompileListToSourceSets(ROOT).compute(List.of(
                gradle("ide-client", List.of(), List.of(), 0, 25),
                gradle("intellij", List.of(jar("ide-client"), EXTERNAL_JAR), null, 0, 25)));
        assertTrue(dependencyNames(named(r, "intellij/main")).contains("ide-client/main"),
                "intellij reads ide-client through its jar on -classpath: " + dependencyNames(named(r, "intellij/main")));
        assertFalse(jarNames(r).contains("ide-client-0.9.1.jar"),
                "the sibling's jar must not ALSO be a library, or its types are in the parse twice: " + jarNames(r));
        assertTrue(jarNames(r).contains("client-2.0.jar"), "CONTROL: an external jar stays a library");
    }

    @DisplayName("CONTROL: a test-fixtures jar is not its module's main output, and stays a library")
    @Test
    public void aClassifiedJarIsNotClaimedByMain() {
        String fixtures = ROOT + "/common/build/libs/common-0.9.1-test-fixtures.jar";
        CompileListToSourceSets.Result r = new CompileListToSourceSets(ROOT).compute(List.of(
                gradle("common", List.of(), List.of(), 0, 25),
                gradle("user", List.of(fixtures), null, 0, 25)));
        assertFalse(dependencyNames(named(r, "user/main")).contains("common/main"),
                "claimed by common/main, the fixtures would leave the parse altogether");
        assertTrue(jarNames(r).contains("common-0.9.1-test-fixtures.jar"), jarNames(r).toString());
    }
}
