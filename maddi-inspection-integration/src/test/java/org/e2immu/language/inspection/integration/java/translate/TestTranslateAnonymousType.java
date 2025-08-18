package org.e2immu.language.inspection.integration.java.translate;

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
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.nio.file.FileVisitResult;

import static org.junit.jupiter.api.Assertions.*;

public class TestTranslateAnonymousType extends CommonTest {

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
                public void method(Path path) {
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
        TypeInfo X = javaInspector.parse(INPUT1);
        TypeInfo fileVisitResult = javaInspector.compiledTypesManager().getOrLoad(FileVisitResult.class);
        FieldInfo continueField = fileVisitResult.getFieldByName("CONTINUE", true);
        FieldInfo siblingsField = fileVisitResult.getFieldByName("SKIP_SIBLINGS", true);
        FieldReference frContinue = javaInspector.runtime().newFieldReference(continueField);
        FieldReference frSiblings = javaInspector.runtime().newFieldReference(siblingsField);
        assertEquals("FileVisitResult.SKIP_SIBLINGS",
                frSiblings.print(javaInspector.runtime().qualificationQualifyFromPrimaryType()).toString());

        TranslationMap tm = javaInspector.runtime().newTranslationMapBuilder()
                .setClearAnalysis(true)
                .put(frContinue, frSiblings)
                .build();

        {
            MethodInfo xMethod = X.findUniqueMethod("method", 1);
            ExpressionAsStatement xEas = (ExpressionAsStatement) xMethod.methodBody().statements().get(0);
            MethodCall xMc = (MethodCall) xEas.expression();
            ConstructorCall xCc = (ConstructorCall) xMc.parameterExpressions().get(3);
            TypeInfo xAnon = xCc.anonymousClass();
            assertEquals("a.b.X.$0", xAnon.fullyQualifiedName());
            assertSame(X, xAnon.compilationUnitOrEnclosingType().getRight());
            MethodInfo xPre = xAnon.findUniqueMethod("preVisitDirectory", 2);
            ParameterInfo xPreDir = xPre.parameters().get(0);
            assertSame(xPre, xPreDir.methodInfo());
            assertSame(xAnon, xPreDir.typeInfo());
        }
        {
            TypeInfo translated = X.translate(tm).getFirst();
            assertNotSame(X, translated);
            MethodInfo tMethod = translated.findUniqueMethod("method", 1);
            ExpressionAsStatement tEas = (ExpressionAsStatement) tMethod.methodBody().statements().get(0);
            MethodCall tMc = (MethodCall) tEas.expression();
            ConstructorCall tCc = (ConstructorCall) tMc.parameterExpressions().get(3);
            TypeInfo tAnon = tCc.anonymousClass();
            assertEquals("a.b.X.$0", tAnon.fullyQualifiedName());
            assertSame(translated, tAnon.compilationUnitOrEnclosingType().getRight());
            MethodInfo tPre = tAnon.findUniqueMethod("preVisitDirectory", 2);
            ParameterInfo tPreDir = tPre.parameters().get(0);
            assertSame(tPre, tPreDir.methodInfo());
            assertSame(tAnon, tPreDir.typeInfo());

            Statement s0 = tPre.methodBody().statements().get(0);
            Expression e0 = s0.expression();
            assertEquals("FileVisitResult.SKIP_SIBLINGS", e0.toString());
        }
    }
}
