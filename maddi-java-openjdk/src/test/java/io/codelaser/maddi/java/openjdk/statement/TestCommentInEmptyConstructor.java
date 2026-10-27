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

package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A comment that is the only thing in a body, or that follows the body's last statement, must survive parsing:
 * printing the member gives it back.
 * <p>
 * ⛔ In a CONSTRUCTOR it does not. {@code C() { // inside }} parses to a constructor with no comment anywhere -- not
 * on the constructor, not on its body (leading or trailing), not on the implicit {@code super()} -- and prints as
 * {@code C(){}}. The same body in a method keeps its comment, which is the control below. A tool that rewrites the
 * constructor (a dependency-injection migration replacing it by an injected one) therefore cannot see the comment
 * and deletes it with the constructor.
 */
public class TestCommentInEmptyConstructor extends CommonTest {

    @Language("java")
    private static final String CONSTRUCTOR = """
            package a.b;
            class C {
              C() {
                // inside
              }
            }
            """;

    @Language("java")
    private static final String METHOD = """
            package a.b;
            class M {
              void m() {
                // inside
              }
            }
            """;

    @Language("java")
    private static final String CONSTRUCTOR_COMMENT_LAST = """
            package a.b;
            class D {
              D() {
                int i = 0;
                // after
              }
            }
            """;

    @Language("java")
    private static final String METHOD_COMMENT_LAST = """
            package a.b;
            class N {
              void n() {
                int i = 0;
                // after
              }
            }
            """;

    @DisplayName("⛔ a comment alone in a constructor's body is kept")
    @Test
    public void constructor() {
        TypeInfo typeInfo = scan("a.b.C", CONSTRUCTOR);
        MethodInfo constructor = typeInfo.constructors().getFirst();
        assertEquals("C(){// inside\n}", constructor.print(runtime.qualificationQualifyFromPrimaryType()).toString(),
                "first: the constructor as it must print; second: as maddi prints it");
    }

    @DisplayName("control: a comment alone in a method's body is kept")
    @Test
    public void method() {
        TypeInfo typeInfo = scan("a.b.M", METHOD);
        MethodInfo method = typeInfo.findUniqueMethod("m", 0);
        assertEquals("void m(){// inside\n}", method.print(runtime.qualificationQualifyFromPrimaryType()).toString(),
                "first: the method as it must print; second: as maddi prints it");
    }

    @DisplayName("⛔ a comment after the last statement of a constructor's body is kept")
    @Test
    public void constructorCommentAfterLastStatement() {
        TypeInfo typeInfo = scan("a.b.D", CONSTRUCTOR_COMMENT_LAST);
        MethodInfo constructor = typeInfo.constructors().getFirst();
        assertEquals("D(){int i=0;// after\n}", constructor.print(runtime.qualificationQualifyFromPrimaryType()).toString(),
                "first: the constructor as it must print; second: as maddi prints it");
    }

    @DisplayName("control: a comment after the last statement of a method's body is kept")
    @Test
    public void methodCommentAfterLastStatement() {
        TypeInfo typeInfo = scan("a.b.N", METHOD_COMMENT_LAST);
        MethodInfo method = typeInfo.findUniqueMethod("n", 0);
        assertEquals("void n(){int i=0;// after\n}", method.print(runtime.qualificationQualifyFromPrimaryType()).toString(),
                "first: the method as it must print; second: as maddi prints it");
    }
}
