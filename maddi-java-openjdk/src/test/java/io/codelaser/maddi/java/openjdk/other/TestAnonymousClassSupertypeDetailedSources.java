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

package org.e2immu.language.java.openjdk.other;

import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An anonymous-class creation ({@code new Supertype(){...}}) must expose the WRITTEN supertype's source token in the
 * {@code ConstructorCall}'s detailed sources, exactly as an ordinary {@code new Supertype(...)} does. The javac scanner
 * built the anonymous branch with {@code convert(type)} (resolved type, no detailed-source side effect) rather than
 * {@code convertTree(tree, dsb)}, so the supertype token was missing -- a dependent-side "move type" rewrite of an
 * anonymous-class supertype FQN then found nothing to retarget and left the FQN stale (ES carve StableApiWrappers:
 * interface anon supertypes such as {@code new org.elasticsearch...CharFilterFactory(){}} not repackaged).
 */
public class TestAnonymousClassSupertypeDetailedSources extends CommonTest {

    private static ConstructorCall returnedConstructorCall(TypeInfo type, String methodName) {
        Statement s = type.findUniqueMethod(methodName, 0).methodBody().statements().getFirst();
        Expression e = s instanceof ReturnStatement rs ? rs.expression() : s.expression();
        return (ConstructorCall) e;
    }

    @DisplayName("interface supertype: new java.lang.Runnable(){} exposes the Runnable FQN token in detailed sources")
    @Test
    public void interfaceAnonSupertypeHasDetailedSource() {
        @Language("java")
        String input = """
                package a.b;
                class X {
                    Object make() {
                        return new java.lang.Runnable() {
                            @Override
                            public void run() {
                            }
                        };
                    }
                }
                """;
        TypeInfo X = scan("a.b.X", input);
        ConstructorCall cc = returnedConstructorCall(X, "make");
        assertNotNull(cc.anonymousClass(), "expected an anonymous class");
        assertNull(cc.constructor(), "an interface anonymous class has no constructor");
        TypeInfo supertype = cc.parameterizedType().typeInfo();
        assertEquals("java.lang.Runnable", supertype.fullyQualifiedName());
        List<Source> tokens = cc.source().detailedSources().details(supertype);
        assertFalse(tokens.isEmpty(),
                "the anonymous-class supertype FQN must carry a detailed source (was empty -> not retargetable)");
        // 'new java.lang.Runnable' -> the type token spans 'java.lang.Runnable' on line 4
        assertEquals(4, tokens.getFirst().beginLine());
    }

    @DisplayName("class supertype: new Object(){} also exposes the supertype token (no regression on the class path)")
    @Test
    public void classAnonSupertypeHasDetailedSource() {
        @Language("java")
        String input = """
                package a.b;
                class X {
                    Object make() {
                        return new java.lang.Object() {
                            @Override
                            public String toString() {
                                return "x";
                            }
                        };
                    }
                }
                """;
        TypeInfo X = scan("a.b.X", input);
        ConstructorCall cc = returnedConstructorCall(X, "make");
        assertNotNull(cc.anonymousClass(), "expected an anonymous class");
        TypeInfo supertype = cc.parameterizedType().typeInfo();
        assertEquals("java.lang.Object", supertype.fullyQualifiedName());
        assertFalse(cc.source().detailedSources().details(supertype).isEmpty(),
                "the anonymous-class supertype FQN must carry a detailed source");
    }
}
