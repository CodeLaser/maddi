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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A build that states {@code -source N} below the running JDK compiled its API against its own, older platform;
 * the parse compiles it against the running one, and every type removed in between stops resolving. The cost is
 * paid in edges that are never created, so nothing downstream reads as wrong — Cassandra lost
 * {@code jdk.internal.ref.Cleaner} and 12 types left the giant while the graph verbs answered
 * {@code messages: []}.
 * <p>
 * Leaving the release unset stays correct ({@link CompileInvocation#effectiveRelease()} carries the 391
 * compilation units that reading {@code -source} as the API costs). ⭐ What changes is that the case is now
 * SAID OUT LOUD instead of sharing an {@code info} line with the harmless one.
 * <p>
 * ⚠ The controls are the point of this class: a warning that fires whenever a release is absent would be
 * noise on every Gradle corpus in the workspace, and would be ignored by the time it mattered.
 */
public class TestStatedSourceOlderThanRunningJdk {

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

    private static Invocation inv(int release, int sourceRelease) {
        return new Invocation("/ws/a/build/classes/java/main", List.of("/ws/a/src/main/java"), List.of(),
                List.of(), release, sourceRelease);
    }

    @DisplayName("⭐ the Cassandra shape: -source 17, no --release, parsed on JDK 26")
    @Test
    public void theCassandraShapeIsWarnedAbout() {
        String warning = CompileListToInputConfiguration
                .statedSourceOlderThanRunningJdk(List.of(inv(0, 17)), 26);
        assertNotNull(warning, "a build that compiled against 17 and is parsed on 26 must not be silent");
        // ⚠ the operator has to be able to ACT on it: which level, which JDK, and what to set
        assertTrue(warning.contains("17") && warning.contains("26"),
                () -> "name both platforms: " + warning);
        assertTrue(warning.contains("--jre"), () -> "name the remedy: " + warning);
    }

    @DisplayName("CONTROL: -source AT the running JDK is not warned about — nothing has been removed yet")
    @Test
    public void aCurrentSourceLevelIsSilent() {
        assertNull(CompileListToInputConfiguration.statedSourceOlderThanRunningJdk(List.of(inv(0, 26)), 26));
    }

    @DisplayName("CONTROL: -source ABOVE the running JDK is not this defect")
    @Test
    public void aNewerSourceLevelIsSilent() {
        assertNull(CompileListToInputConfiguration.statedSourceOlderThanRunningJdk(List.of(inv(0, 26)), 21));
    }

    /*
    ⛔ THE LOAD-BEARING CONTROL. --release 17 pins the API through ct.sym, so the parse reads java.base from the
    17 band and the build did too: there is no downgrade and nothing to say. Warning here would fire on every
    corpus that does the RIGHT thing.
     */
    @DisplayName("CONTROL: a stated --release is the API, and is not warned about")
    @Test
    public void aStatedReleaseIsSilent() {
        assertNull(CompileListToInputConfiguration.statedSourceOlderThanRunningJdk(List.of(inv(17, 17)), 26));
    }

    @DisplayName("CONTROL: an invocation that states NOTHING keeps the old info line, and no warning")
    @Test
    public void asilentInvocationIsSilent() {
        assertNull(CompileListToInputConfiguration.statedSourceOlderThanRunningJdk(List.of(inv(0, 0)), 26));
    }

    @DisplayName("it counts the EXPOSED invocations, not all of them")
    @Test
    public void theCountIsOfTheExposedOnly() {
        String warning = CompileListToInputConfiguration.statedSourceOlderThanRunningJdk(
                List.of(inv(0, 17), inv(21, 21), inv(0, 0)), 26);
        assertNotNull(warning);
        assertTrue(warning.startsWith("1 of 3 "),
                () -> "only the -source 17 invocation is exposed; the other two are fine: " + warning);
    }

    @DisplayName("several stated levels are all named, and the remedy points at the LOWEST")
    @Test
    public void theRemedyPointsAtTheLowest() {
        String warning = CompileListToInputConfiguration.statedSourceOlderThanRunningJdk(
                List.of(inv(0, 17), inv(0, 21)), 26);
        assertNotNull(warning);
        assertTrue(warning.contains("[17, 21]"), () -> "name every level: " + warning);
        // ⚠ a JDK 21 would still drop what was removed between 17 and 21; only the lowest is safe for all
        assertTrue(warning.contains("JDK 17"), () -> "the remedy must clear the LOWEST level: " + warning);
    }
}
