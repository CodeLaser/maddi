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
