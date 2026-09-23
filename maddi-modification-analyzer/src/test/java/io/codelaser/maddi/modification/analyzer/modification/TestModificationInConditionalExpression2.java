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

package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⚠ The NEGATIVE control for the conditional-expression fix, taken verbatim from the shape the clone-bench
 * corpus moved on (`Function5190901_file1956686.getParameterPositions`). Restoring the arms' records must not
 * invent a modification: both arms here are read-only, so the method stays non-modifying.
 */
public class TestModificationInConditionalExpression2 extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.Collections;
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            public class Y {
                private final Map<String, List<Integer>> paramPos = new HashMap<>();
                public List<Integer> getParameterPositions(String name) {
                    final List<Integer> integerList = paramPos.get(name);
                    return integerList == null ? Collections.<Integer>emptyList() : integerList;
                }
                public boolean has(String name, Integer i) {
                    final List<Integer> integerList = paramPos.get(name);
                    return integerList == null ? false : integerList.contains(i);
                }
                public List<Integer> viaUnmodifiable(String name) {
                    final List<Integer> integerList = paramPos.get(name);
                    return integerList == null ? Collections.<Integer>emptyList()
                            : Collections.unmodifiableList(integerList);
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo Y = javaInspector.parse("a.b.Y", INPUT);
        List<Info> analysisOrder = prepWork(Y);
        analyzer.go(analysisOrder);
        assertFalse(Y.findUniqueMethod("getParameterPositions", 1).isModifying(),
                "both arms only READ: restoring the arms' records must not invent a modification");
        assertFalse(Y.findUniqueMethod("has", 2).isModifying(),
                "`contains` is @NotModified[O]: a read-only call in an arm stays read-only");

        // ⚠ ...and the one that is NOT the fix's doing. `Collections.unmodifiableList` carries
        // `@Independent[M]` and no `@NotModified`, so the archive says it MODIFIES its argument
        // (`Mark argument primary integerList as modified by java.util.Collections.unmodifiableList`).
        // Before the fix the ternary discarded that record and the method read as non-modifying; it is the
        // hint that decides this, not the conditional. Asserted as-is so that a later hint change is visible
        // here rather than only in the clone-bench ratchet, where it cost ten reverse divergences.
        assertTrue(Y.findUniqueMethod("viaUnmodifiable", 1).isModifying(),
                "the archive marks unmodifiableList's argument modified; the ternary must not hide it");
    }
}
