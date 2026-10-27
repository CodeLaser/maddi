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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import io.codelaser.maddi.inspection.resource.ParseResultImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestRecord extends CommonTest {

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import io.codelaser.maddi.annotation.method.GetSet;
            import java.util.ArrayList;
            import java.util.HashSet;
            import java.util.Set;
            import java.util.List;
            class X {
                interface R {
                    @GetSet Set<Integer> set();
                    int i();
                }
                record RI(Set<Integer> set, int i, List<String> list) implements R {}
            
                void setAdd(R r) {
                    r.set().add(r.i());
                }
            
                void method() {
                    List<String> l = new ArrayList<>();
                    Set<Integer> s = new HashSet<>();
                    R r = new RI(s, 3, l);
                    setAdd(r); // at this point, s1 should have been modified, via???
                }
            }
            """;

    @DisplayName("combination test of record implementing an interface")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

        // check that @GetSet works at the level R
        TypeInfo R = X.findSubType("R");
        MethodInfo Rset = R.findUniqueMethod("set", 0);
        FieldInfo RsetField = R.getFieldByName("set", true);
        assertSame(RsetField, Rset.getSetField().field());

        // check that @GetSet works at the level RI
        TypeInfo RI = X.findSubType("RI");
        MethodInfo RIset = RI.findUniqueMethod("set", 0);
        FieldInfo RIsetField = RI.getFieldByName("set", true);
        assertSame(RIsetField, RIset.getSetField().field());

        // also, verify the override
        assertTrue(RIset.overrides().contains(Rset));

        ParseResult parseResult = new ParseResultImpl(Set.of(X), Map.of(), null);
        assertEquals("[a.b.X.RI]", parseResult.descendants(R, false).toString());
    }
}
