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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.java.openjdk.CommonTest;
import io.codelaser.maddi.java.openjdk.ScanCompilationUnits;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * #40: a class literal whose qualifier javac could not resolve carries an error symbol: a ClassSymbol named
 * {@code class}, owned by the qualifier. It used to be materialised as a member type of the qualifier, or, when
 * the qualifier was already committed, to kill the whole parse with an UnsupportedOperationException. It is an
 * unresolved symbol: the unit is dropped with a warning, the run proceeds.
 */
public class TestClassLiteralOnUnresolvedType extends CommonTest {

    @DisplayName("a class literal on an unresolvable type drops the unit tolerably")
    @Test
    public void test() {
        ScanCompilationUnits.Result result = scan(true, Map.of("a.b.E", """
                package a.b;
                public class E {
                    Object m() {
                        return a.b.gone.X.class;
                    }
                }
                """));
        assertEquals(1, result.failures().size());
        ScanCompilationUnits.CompilationUnitFailure failure = result.failures().getFirst();
        assertTrue(failure.tolerable(), "an erroneous class symbol is a partial-classpath miss, not a hard error: "
                                       + failure.detail());
    }
}
