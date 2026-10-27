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
package io.codelaser.maddi.java.openjdk.translate;

import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A local variable carries its assignment expression, and the declaration and every reference share one
 * {@code LocalVariable}. When a translation changes that expression -- here a constant inside a lambda -- the
 * declaration used to get a new variable holding the new lambda while every reference kept the old one, whose
 * implementation type belongs to the type that was translated AWAY. A rewire of the result then met a lambda
 * whose type no longer had a single abstract method (the jfocus transform, 2026-09-21).
 */
public class TestTranslateLocalHoldingLambda extends CommonTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            import java.util.Map;
            import java.util.function.Consumer;
            class X {
                void method(Map<String, String> a, Map<String, String> b) {
                    Consumer<Map<String, String>> put = m -> m.put("k", "v");
                    put.accept(a);
                    put.accept(b);
                }
            }
            """;

    @DisplayName("the references follow the declaration: one variable, one lambda, owned by the translated type")
    @Test
    public void test() {
        TypeInfo X = scan("a.b.X", INPUT);
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .setClearAnalysis(true)
                .put(runtime.newStringConstant("k"), runtime.newStringConstant("j"))
                .build();
        TypeInfo translated = X.translate(tm).getFirst();
        assertNotSame(X, translated);
        MethodInfo method = translated.findUniqueMethod("method", 2);
        LocalVariableCreation lvc = (LocalVariableCreation) method.methodBody().statements().get(0);
        LocalVariable declared = lvc.localVariable();
        Lambda lambda = (Lambda) declared.assignmentExpression();
        assertTrue(lambda.methodInfo().methodBody().toString().contains("\"j\""), lambda.methodInfo().methodBody().toString());
        assertSame(translated, lambda.methodInfo().typeInfo().compilationUnitOrEnclosingType().getRight(),
                "the lambda's implementation type belongs to the translated X");
        for (int i = 1; i <= 2; i++) {
            ExpressionAsStatement eas = (ExpressionAsStatement) method.methodBody().statements().get(i);
            MethodCall call = (MethodCall) eas.expression();
            LocalVariable referenced = (LocalVariable) ((VariableExpression) call.object()).variable();
            assertSame(declared, referenced, "statement " + i + " refers to the declared variable, not to a copy");
        }
    }
}
