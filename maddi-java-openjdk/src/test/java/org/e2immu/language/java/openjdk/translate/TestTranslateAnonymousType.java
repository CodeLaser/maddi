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

package org.e2immu.language.java.openjdk.translate;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestTranslateAnonymousType extends CommonTest {

    public TestTranslateAnonymousType() {
        super(List.of("java.base::java.nio.file"));
    }

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.nio.file.FileVisitResult;
            import java.nio.file.FileVisitor;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.nio.file.attribute.BasicFileAttributes;
            import java.util.Set;
            class X {
                public void method(Path path) throws IOException {
                    Files.walkFileTree(path, Set.of(), Integer.MAX_VALUE, new FileVisitor<Path>() {
                        @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)throws IOException {
                            return null;
                        }
                        @Override public FileVisitResult visitFileFailed(Path file, IOException exc)throws IOException {
                            return null;
                        }
                        @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc)throws IOException {
                            return null;
                        }
                    });
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = scan("a.b.X", INPUT1);
        TypeInfo fileVisitResult = classSymbolScanner.getType("java.nio.file.FileVisitResult");
        FieldInfo continueField = fileVisitResult.getFieldByName("CONTINUE", true);
        FieldInfo siblingsField = fileVisitResult.getFieldByName("SKIP_SIBLINGS", true);
        FieldReference frContinue = runtime.newFieldReference(continueField);
        FieldReference frSiblings = runtime.newFieldReference(siblingsField);
        assertEquals("FileVisitResult.SKIP_SIBLINGS",
                frSiblings.print(runtime.qualificationQualifyFromPrimaryType()).toString());

        TranslationMap tm = runtime.newTranslationMapBuilder()
                .setClearAnalysis(true)
                .put(frContinue, frSiblings)
                .build();

        {
            MethodInfo xMethod = X.findUniqueMethod("method", 1);
            ExpressionAsStatement xEas = (ExpressionAsStatement) xMethod.methodBody().statements().getFirst();
            MethodCall xMc = (MethodCall) xEas.expression();
            ConstructorCall xCc = (ConstructorCall) xMc.parameterExpressions().get(3);
            TypeInfo xAnon = xCc.anonymousClass();
            assertEquals("a.b.X.$0", xAnon.fullyQualifiedName());
            assertSame(X, xAnon.compilationUnitOrEnclosingType().getRight());
            MethodInfo xPre = xAnon.findUniqueMethod("preVisitDirectory", 2);
            ParameterInfo xPreDir = xPre.parameters().getFirst();
            assertSame(xPre, xPreDir.methodInfo());
            assertSame(xAnon, xPreDir.typeInfo());
        }
        {
            TypeInfo translated = X.translate(tm).getFirst();
            assertNotSame(X, translated);
            MethodInfo tMethod = translated.findUniqueMethod("method", 1);
            ExpressionAsStatement tEas = (ExpressionAsStatement) tMethod.methodBody().statements().getFirst();
            MethodCall tMc = (MethodCall) tEas.expression();
            ConstructorCall tCc = (ConstructorCall) tMc.parameterExpressions().get(3);
            TypeInfo tAnon = tCc.anonymousClass();
            assertEquals("a.b.X.$0", tAnon.fullyQualifiedName());
            assertSame(translated, tAnon.compilationUnitOrEnclosingType().getRight());
            MethodInfo tPre = tAnon.findUniqueMethod("preVisitDirectory", 2);
            ParameterInfo tPreDir = tPre.parameters().getFirst();
            assertSame(tPre, tPreDir.methodInfo());
            assertSame(tAnon, tPreDir.typeInfo());

            Statement s0 = tPre.methodBody().statements().getFirst();
            Expression e0 = s0.expression();
            assertEquals("FileVisitResult.SKIP_SIBLINGS", e0.toString());
        }
    }
}
