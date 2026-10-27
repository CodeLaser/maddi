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

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A record's canonical-constructor parameters are synthesised by javac; their tree positions are unreliable, so the
 * detailed sources built from them are wrong (type range off by a character, name detail landing in the 'record'
 * keyword, truncated whole-parameter source). The corresponding record-component <em>field</em> is scanned from the
 * real component declaration and is correct. The scanner mirrors the field's source onto the parameter; these tests
 * pin that down.
 */
public class TestRecordComponentParameterSource extends CommonTest {

    private static String detail(Source s, Object key) {
        Source d = s.detailedSources() == null ? null : s.detailedSources().detail(key);
        return d == null ? null : d.toString();
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public record R(String first, int second) { }
            """;
    //                     1234567890123456789012345678901234567890
    //  'String' cols 17-22, 'first' 24-28, 'int' 31-33, 'second' 35-40   (all on line 2)

    @DisplayName("implicit canonical constructor: parameter sources mirror the component fields")
    @Test
    public void test1() {
        TypeInfo R = scan("a.b.R", INPUT1);
        MethodInfo ctor = R.findConstructor(2);
        assertEquals(2, ctor.parameters().size());

        for (String comp : new String[]{"first", "second"}) {
            FieldInfo field = R.getFieldByName(comp, true);
            ParameterInfo param = ctor.parameters().stream().filter(p -> p.name().equals(comp)).findFirst().orElseThrow();

            // the parameter now carries a source with detailed sources...
            assertNotNull(param.source());
            assertNotNull(param.source().detailedSources(), "no detailed sources on parameter " + comp);

            // ...and it mirrors the (correct) field's source, type detail and name detail
            assertEquals(field.source().toString(), param.source().toString(), "source of " + comp);
            assertEquals(detail(field.source(), field.type()), detail(param.source(), param.parameterizedType()),
                    "type detail of " + comp);
            assertEquals(detail(field.source(), field.name()), detail(param.source(), param.name()),
                    "name detail of " + comp);
        }

        // absolute anchors, so a regression in the *field* itself is also caught
        ParameterInfo first = ctor.parameters().getFirst();
        assertEquals("-@2:17-2:28", first.source().toString());               // 'String first'
        assertEquals("-@2:17-2:22", detail(first.source(), first.parameterizedType())); // 'String'
        assertEquals("-@2:24-2:28", detail(first.source(), first.name()));    // 'first'

        ParameterInfo second = ctor.parameters().get(1);
        assertEquals("-@2:31-2:40", second.source().toString());              // 'int second'
        assertEquals("-@2:31-2:33", detail(second.source(), second.parameterizedType())); // 'int'
        assertEquals("-@2:35-2:40", detail(second.source(), second.name()));  // 'second'
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public record R(String first, int second) {
                public R {
                    if (first == null) throw new NullPointerException();
                }
            }
            """;

    @DisplayName("compact canonical constructor: parameter sources mirror the component fields")
    @Test
    public void test2() {
        TypeInfo R = scan("a.b.R", INPUT2);
        MethodInfo ctor = R.findConstructor(2);
        assertEquals(2, ctor.parameters().size());

        for (String comp : new String[]{"first", "second"}) {
            FieldInfo field = R.getFieldByName(comp, true);
            ParameterInfo param = ctor.parameters().stream().filter(p -> p.name().equals(comp)).findFirst().orElseThrow();
            assertNotNull(param.source().detailedSources(), "no detailed sources on parameter " + comp);
            assertEquals(field.source().toString(), param.source().toString(), "source of " + comp);
            assertEquals(detail(field.source(), field.type()), detail(param.source(), param.parameterizedType()),
                    "type detail of " + comp);
            assertEquals(detail(field.source(), field.name()), detail(param.source(), param.name()),
                    "name detail of " + comp);
        }
    }

    @DisplayName("record type carries END_OF_PARAMETER_LIST (closing ')' of the component list)")
    @Test
    public void test3() {
        TypeInfo R = scan("a.b.R", INPUT1);
        // 'public record R(String first, int second) { }': the ')' closing the component list is at column 41
        Source end = R.source().detailedSources().detail(DetailedSources.END_OF_PARAMETER_LIST);
        assertNotNull(end, "record type must record the closing ')' of its component list");
        assertEquals("@2:41-2:41", end.toString());
    }
}
