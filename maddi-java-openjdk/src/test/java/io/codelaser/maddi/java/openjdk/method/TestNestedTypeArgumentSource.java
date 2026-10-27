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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The nested half of {@link TestParameterInfoSource} and {@link TestReturnTypeSource}. Those pin that a
 * method created from its symbol before its declaration is reached still resolves the source of its
 * parameter type and its return type. This pins the same for the TYPE ARGUMENTS inside them, which the
 * one-level fix-up did not reach.
 * <p>
 * The trigger is ordinary: a body that calls a method declared LOWER IN THE SAME TYPE. Move the callee
 * above the caller and every argument resolves; leave it below and none of them does.
 */
public class TestNestedTypeArgumentSource extends CommonTest {

    @Language("java")
    private static final String FORWARD = """
            package a;
            import java.util.Map;

            class A {
                Map<String, A> caller(Map<String, A> in) {
                    return callee(in);
                }

                Map<String, A> callee(Map<String, A> in) {
                    return in;
                }
            }
            """;

    @Language("java")
    private static final String BACKWARD = """
            package a;
            import java.util.Map;

            class B {
                Map<String, B> callee(Map<String, B> in) {
                    return in;
                }

                Map<String, B> caller(Map<String, B> in) {
                    return callee(in);
                }
            }
            """;

    @DisplayName("A forward-referenced method resolves the source of its parameter's type ARGUMENTS")
    @Test
    public void test1() {
        TypeInfo A = scan("a.A", FORWARD);
        MethodInfo callee = A.findUniqueMethod("callee", 1);
        ParameterInfo pi = callee.parameters().getFirst();
        assertNotNull(pi.source());

        ParameterizedType written = pi.parameterizedType();
        // the outer type already resolved before this fix
        assertNotNull(pi.source().detailedSources().detail(written));
        // 'String' and 'A' on line 9: 'Map<String, A> callee(Map<String, A> in) {'
        assertNotNull(pi.source().detailedSources().detail(written.parameters().get(0)),
                "the FIRST type argument of a forward-referenced parameter has no source");
        assertNotNull(pi.source().detailedSources().detail(written.parameters().get(1)),
                "the SECOND type argument of a forward-referenced parameter has no source");
        assertEquals("-@9:31-9:36",
                pi.source().detailedSources().detail(written.parameters().get(0)).toString());
        assertEquals("-@9:39-9:39",
                pi.source().detailedSources().detail(written.parameters().get(1)).toString());

        // ...and the same for the RETURN type, whose fix-up has the identical shape
        ParameterizedType returnType = callee.returnType();
        assertNotNull(callee.source().detailedSources().detail(returnType.parameters().get(0)),
                "the return type's first argument has no source");
        assertNotNull(callee.source().detailedSources().detail(returnType.parameters().get(1)),
                "the return type's second argument has no source");
    }

    @DisplayName("...exactly as it already did when the callee is declared above its caller")
    @Test
    public void test2() {
        TypeInfo B = scan("a.B", BACKWARD);
        MethodInfo callee = B.findUniqueMethod("callee", 1);
        ParameterInfo pi = callee.parameters().getFirst();
        ParameterizedType written = pi.parameterizedType();
        assertNotNull(pi.source().detailedSources().detail(written.parameters().get(0)));
        assertNotNull(pi.source().detailedSources().detail(written.parameters().get(1)));
        // ⚠ ONE source, not a list: the fix must not key an argument that convertTree already keyed.
        assertEquals(1, pi.source().detailedSources().details(written.parameters().get(0)).size(),
                "the argument was keyed twice; details() consumers would see a duplicate");
        assertEquals(1, pi.source().detailedSources().details(written.parameters().get(1)).size(),
                "the argument was keyed twice; details() consumers would see a duplicate");
    }
}
