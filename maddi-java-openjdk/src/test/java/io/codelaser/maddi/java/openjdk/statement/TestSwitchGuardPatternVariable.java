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

package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A pattern variable introduced by the GUARD of a switch arm is in scope in that arm's body (JLS 6.3.1 flow
 * scoping), and must therefore still be on the element stack when the body is parsed.
 * <p>
 * ⛔ It was not. The scanner pushed a scope holding the LABEL's pattern variables, scanned the guard into it,
 * and popped it again before parsing the body — so a variable the guard introduced was created and immediately
 * discarded, and resolving it in the body threw
 * {@code UnsupportedOperationException: Cannot find element 'ai' on stack}. That aborts the compilation unit,
 * and one aborted unit refuses the whole {@code ParseResult}: this single line rejected a 354-source-set,
 * 23,497-file Elasticsearch parse
 * ({@code x-pack/plugin/esql-datasource-parquet/.../PageColumnReaderCorrectnessTests.java:558}).
 * <p>
 * ⚠ <b>It fails EVERY time, not in a corner.</b> I first supposed the arm needed a label binding as well, so
 * that a scope existed to be popped, and that a bare {@code case Integer _} would leak its guard binding into
 * the enclosing block and resolve by accident. <b>Measured against the pre-fix code: all four tests below fail
 * with {@code Cannot find element 's' on stack}, including the {@code _} one</b> — an unnamed pattern still
 * produces a binding, and Java allows a guard only on a pattern label, so there is always a scope to pop.
 * The leak was a hypothesis and the fixture refuted it.
 */
public class TestSwitchGuardPatternVariable extends CommonTest {

    // the ES shape: the label binds `i`, the guard binds `s`, and the BODY uses the guard's binding
    @Language("java")
    private static final String EXPRESSION_BODY = """
            package a.b;
            class C {
                static int method(Object o, Object other) {
                    return switch (o) {
                        case Integer i when other instanceof String s -> i + s.length();
                        default -> 0;
                    };
                }
            }
            """;

    @Language("java")
    private static final String BLOCK_BODY = """
            package a.b;
            class C {
                static int method(Object o, Object other) {
                    switch (o) {
                        case Integer i when other instanceof String s -> { return i + s.length(); }
                        default -> { return 0; }
                    }
                }
            }
            """;

    /** An UNNAMED label pattern. It still binds, so this fails pre-fix exactly like the named cases. */
    @Language("java")
    private static final String NO_LABEL_BINDING = """
            package a.b;
            class C {
                static int method(Object o, Object other) {
                    int r = switch (o) {
                        case Integer _ when other instanceof String s -> s.length();
                        default -> 0;
                    };
                    return r;
                }
            }
            """;

    @DisplayName("a guard's pattern variable is visible in an expression-bodied arm")
    @Test
    public void guardBindingVisibleInExpressionBody() {
        TypeInfo typeInfo = scan("a.b.C", EXPRESSION_BODY);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        assertNotNull(method.methodBody());
        String printed = method.methodBody().print(runtime.qualificationQualifyFromPrimaryType()).toString();
        assertTrue(printed.contains("s.length()"), "the guard's binding has to reach the body: " + printed);
    }

    @DisplayName("a guard's pattern variable is visible in a block-bodied arm")
    @Test
    public void guardBindingVisibleInBlockBody() {
        TypeInfo typeInfo = scan("a.b.C", BLOCK_BODY);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        assertNotNull(method.methodBody());
        String printed = method.methodBody().print(runtime.qualificationQualifyFromPrimaryType()).toString();
        assertTrue(printed.contains("s.length()"), "the guard's binding has to reach the body: " + printed);
    }

    @DisplayName("a guard's pattern variable resolves when the label binds nothing")
    @Test
    public void guardBindingWithoutALabelBinding() {
        TypeInfo typeInfo = scan("a.b.C", NO_LABEL_BINDING);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        assertNotNull(method.methodBody());
    }

    /**
     * The other side of the same scope: the arm's binding must not displace a LATER declaration of the same
     * name in the enclosing block. This is the case a too-wide scope would break — the fix pushes one scope per
     * arm and pops it, so the outer {@code String s} is still the outer one.
     */
    @DisplayName("a guard's pattern variable does not outlive its arm")
    @Test
    public void aGuardBindingDoesNotOutliveItsArm() {
        // `s` is declared AFTER the switch: it must be the local, not a leftover from the guard
        @Language("java") String input = """
                package a.b;
                class C {
                    static int method(Object o, Object other) {
                        int r = switch (o) {
                            case Integer i when other instanceof String s -> i + s.length();
                            default -> 0;
                        };
                        String s = "outer";
                        return r + s.length();
                    }
                }
                """;
        TypeInfo typeInfo = scan("a.b.C", input);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        String printed = method.methodBody().print(runtime.qualificationQualifyFromPrimaryType()).toString();
        assertTrue(printed.contains("\"outer\""), "the outer declaration survives: " + printed);
    }
}
