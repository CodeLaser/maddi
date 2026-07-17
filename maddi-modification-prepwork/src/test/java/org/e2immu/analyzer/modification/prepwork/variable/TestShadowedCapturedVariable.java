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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ForEachStatement;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Regression (originally surfaced by Jenkins' {@code hudson.Launcher.decorateByEnv}): an anonymous class captures
 * a local variable of the enclosing method ({@code env}), and one of its methods declares a for-each loop whose
 * loop variable ({@code env}) shadows that captured variable.
 * <p>
 * The captured variable and the loop variable are two distinct {@code LocalVariable} instances that nevertheless
 * share the same {@code fullyQualifiedName()} ("env"). The prep analyzer's {@code MethodAnalyzer} keys its
 * variable maps by FQN, so the two collide; a {@code VariableInfoContainer} then ends up whose own
 * {@code variable()} differs from its best {@code VariableInfo}'s {@code variable()}, tripping the identity
 * assertion in {@code VariableInfoContainerImpl}'s constructor and aborting prep for the method.
 */
public class TestShadowedCapturedVariable extends CommonTest {

    // the collision only arises with the openjdk front-end: it gives the captured local and the shadowing loop
    // variable the same fullyQualifiedName ("env"), whereas the in-house parser disambiguates them. Force the
    // openjdk parser here regardless of the maddi_parser system property, so this guards the real defect.
    @Override
    @BeforeEach
    public void beforeEach() throws IOException, URISyntaxException {
        openJdkParser();
        runtime = javaInspector.runtime();
    }

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.List;
            public class X {
                interface Sink {
                    void run(List<String> items);
                }
                Sink make(String raw) {
                    final String env = raw + "!";
                    return new Sink() {
                        @Override
                        public void run(List<String> items) {
                            StringBuilder e = new StringBuilder(env);
                            if (items != null) {
                                for (String env : items) {
                                    e.append(env);
                                }
                            }
                            System.out.println(e);
                        }
                    };
                }
            }
            """;

    @DisplayName("prep keeps the outer captured 'env' and the shadowing loop 'env' separate")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        assertDoesNotThrow(() -> analyzer.doPrimaryType(X));

        // navigate: make() -> its local 'env' (outer) and the anonymous Sink -> run() -> the for-each 'env' (inner)
        MethodInfo make = X.findUniqueMethod("make", 1);
        LocalVariable outerEnv = ((LocalVariableCreation) make.methodBody().statements().get(0)).localVariable();
        ConstructorCall cc = (ConstructorCall) ((ReturnStatement) make.methodBody().lastStatement()).expression();
        MethodInfo run = cc.anonymousClass().findUniqueMethod("run", 1);

        java.util.List<Statement> runStmts = run.methodBody().statements();
        Statement builderStmt = runStmts.get(0);          // StringBuilder e = new StringBuilder(env);  -- reads outer
        IfElseStatement ifStmt = (IfElseStatement) runStmts.get(1);
        ForEachStatement forEach = (ForEachStatement) ifStmt.block().statements().get(0);
        LocalVariable innerEnv = forEach.initializer().localVariable();
        Statement appendStmt = forEach.block().statements().get(0);   // e.append(env);  -- reads inner
        Statement lastStmt = run.methodBody().lastStatement();        // System.out.println(e);

        assertNotSame(outerEnv, innerEnv, "sanity: they are distinct LocalVariable instances");
        assertEquals(outerEnv.fullyQualifiedName(), innerEnv.fullyQualifiedName(),
                "sanity: openjdk gives them the same fullyQualifiedName -- the source of the bug");

        // the live 'env' before/after the loop is the OUTER; inside the loop body it is the INNER
        assertSame(outerEnv, VariableDataImpl.of(builderStmt).variableInfoContainerOrNull("env").variable());
        assertSame(outerEnv, VariableDataImpl.of(lastStmt).variableInfoContainerOrNull("env").variable());
        assertSame(innerEnv, VariableDataImpl.of(appendStmt).variableInfoContainerOrNull("env").variable());

        // the OUTER 'env' at the last statement: read only where it is actually read (the StringBuilder), never at
        // the inner read; assigned only outside run() (never inside the loop)
        // OUTER 'env' (at the last statement of run()): read only where it is actually read -- the StringBuilder
        // at index 0 -- and never at the inner read (1.0.0.0.0); assigned nowhere inside run(), so the loop's
        // assignment to the inner 'env' does not leak in.
        VariableInfo outerVi = VariableDataImpl.of(lastStmt).variableInfo(outerEnv);
        assertEquals("0", outerVi.reads().toString());
        assertFalse(outerVi.reads().toString().contains("1.0.0"),
                "the outer 'env' must not pick up the inner loop's read");
        assertEquals("D:+, A:[]", outerVi.assignments().toString());
        assertFalse(outerVi.assignments().toString().contains("1.0.0"),
                "the outer 'env' must not pick up the inner loop's assignment");

        // INNER 'env' (inside the loop body): read only at the append (1.0.0.0.0), never at the outer read (0);
        // assigned by the loop (1.0.0), not by anything belonging to the outer.
        VariableInfo innerVi = VariableDataImpl.of(appendStmt).variableInfo(innerEnv);
        assertEquals("1.0.0.0.0", innerVi.reads().toString());
        assertEquals("D:+, A:[1.0.0+E]", innerVi.assignments().toString());
    }
}
