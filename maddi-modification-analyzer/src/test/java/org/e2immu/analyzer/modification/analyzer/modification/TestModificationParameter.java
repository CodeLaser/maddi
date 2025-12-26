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

package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.Reader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModificationParameter extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class Function63498_file16492 {
                private static int method(int[] dest, int offset, int[] x, int len, int y) {
                    long yl =(long)y & 4294967295L;
                    int carry = 0;
                    int j = 0;
            
                    do {
                        long prod =((long)x[j] & 4294967295L) * yl;
                        int prod_low =(int)prod;
                        int prod_high =(int)(prod >> 32);
                        prod_low += carry;
                        carry =((prod_low ^ -2147483648) <(carry ^ -2147483648) ? 1 : 0) + prod_high;
                        int x_j = dest[offset + j];
                        prod_low = x_j - prod_low;
                        if((prod_low ^ -2147483648) >(x_j ^ -2147483648)) { carry++; }
                        dest[offset + j] = prod_low;
                    } while((++j) < len);
            
                    return carry;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        MethodInfo methodInfo = X.findUniqueMethod("method", 5);
        ParameterInfo dest = methodInfo.parameters().getFirst();

        Statement s308 = methodInfo.methodBody().statements().get(3).block().statements().get(8);
        assertInstanceOf(Assignment.class, s308.expression());
        VariableData vd308 = VariableDataImpl.of(s308);
        VariableInfo dest308 = vd308.variableInfo(dest);
        assertTrue(dest308.isModified());

        Statement s4 = methodInfo.methodBody().statements().get(4);
        VariableData vd4 = VariableDataImpl.of(s4);
        VariableInfo dest4 = vd4.variableInfo(dest);
        assertTrue(dest4.isModified());

        assertTrue(dest.isModified());
    }

    @Language("java")
    private static final String INPUT2 = """
            import java.io.IOException;
            import java.io.Reader;
            public class BundleJSONParser {
                private static String readFully(final Reader reader) throws IOException {
                    final char[] arr = new char[1024];
                    final StringBuilder sb = new StringBuilder();
            
                    try(reader) {
                        int numChars;
                        while((numChars = reader.read(arr, 0, arr.length)) > 0) { sb.append(arr, 0, numChars); }
                    }
            
                    return sb.toString();
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);

        TypeInfo readerType = javaInspector.compiledTypesManager().getOrLoad(Reader.class);
        MethodInfo readMethod = readerType.findUniqueMethod("read", 3);
        assertTrue(readMethod.isModifying());

        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        MethodInfo methodInfo = X.findUniqueMethod("readFully", 1);
        ParameterInfo reader = methodInfo.parameters().getFirst();

        Statement s201 = methodInfo.methodBody().statements().get(2).block().statements().get(1);
        VariableInfo viReader201 = VariableDataImpl.of(s201).variableInfo(reader);
        assertTrue(viReader201.isModified());

        VariableData vd = VariableDataImpl.of(methodInfo.methodBody().lastStatement());
        VariableInfo viReader = vd.variableInfo(reader);
        assertEquals("2+0, 2.0.1-E, 2.0.1;E", viReader.reads().toString());
        assertTrue(viReader.isModified());

        assertTrue(reader.isModified());
    }

    @Language("java")
    private static final String INPUT3 = """
            import java.io.File;
            import java.util.List;
            
            class Function18024_file101780 {
                private int findSources(File dir, List<String> args) {
                    File[] files = dir.listFiles();
                    if (files == null || files.length == 0) return 0;
                    int found = 0;
                    for (int i = 0; i < files.length; i++) {
                        File file = files[i];
                        if (file.isDirectory()) {
                            found += findSources(file, args);
                        } else if (file.getName().endsWith(".java")) {
                            args.add(file.toString());
                            found++;
                        }
                    }
                    return found;
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo B = javaInspector.parse(INPUT3);

        TypeInfo file = javaInspector.compiledTypesManager().getOrLoad(File.class);
        MethodInfo listFiles = file.findUniqueMethod("listFiles", 0);
        assertTrue(listFiles.isNonModifying());

        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo findSources = B.findUniqueMethod("findSources", 2);
        ParameterInfo dir = findSources.parameters().getFirst();
        assertEquals("dir", dir.name());

        VariableData vd0 = VariableDataImpl.of(findSources.methodBody().statements().getFirst());
        VariableInfo viDir0 = vd0.variableInfo(dir);
        assertTrue(viDir0.isUnmodified());

        Statement s1 = findSources.methodBody().statements().get(1);
        VariableData vd100 = VariableDataImpl.of(s1.block().statements().getFirst());
        VariableInfo viDir100 = vd100.variableInfo(dir);
        assertTrue(viDir100.isUnmodified());

        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo viDir1 = vd1.variableInfo(dir);
        assertTrue(viDir1.isUnmodified());

        VariableData vd4 = VariableDataImpl.of(findSources.methodBody().statements().get(4));
        VariableInfo viDir4 = vd4.variableInfo(dir);
        assertTrue(viDir4.isUnmodified());

        assertTrue(dir.isUnmodified());
        assertFalse(dir.isModified());

        ParameterInfo p1 = findSources.parameters().get(1);
        assertTrue(p1.isModified());
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.*;
            import java.nio.file.attribute.BasicFileAttributes;
            import java.util.Collections;
            import java.util.LinkedList;
            import java.util.List;
            class X {
                List<String> list(Path path) throws IOException {
                    List<String> list = new LinkedList<>();
            
                    Files.walkFileTree(path,
                            Collections.singleton(FileVisitOption.FOLLOW_LINKS),
                            Integer.MAX_VALUE,
                            new FileVisitor<>() {
                                public FileVisitResult preVisitDirectory(Path p, BasicFileAttributes attrs) {
                                    list.add(p.toString());
                                    return FileVisitResult.CONTINUE;
                                }
            
                                @Override
                                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)  {
                                    list.add(file.toString());
                                    return FileVisitResult.CONTINUE;
                                }
            
                                @Override
                                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
            
                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                                    if(exc instanceof Object) throw new UnsupportedOperationException();
                                    return FileVisitResult.CONTINUE;
                                }
            
                            });
                    return list;
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo B = javaInspector.parse(INPUT4);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
        MethodInfo list = B.findUniqueMethod("list", 1);
        MethodCall walkFileTree = (MethodCall) list.methodBody().statements().get(1).expression();
        ConstructorCall cc = (ConstructorCall) walkFileTree.parameterExpressions().get(3);
        TypeInfo anon = cc.anonymousClass();
        MethodInfo preVisitDirectory = anon.findUniqueMethod("preVisitDirectory", 2);
        VariableData vdLast = VariableDataImpl.of(preVisitDirectory.methodBody().lastStatement());
        assertEquals("""
                a.b.X.$0.preVisitDirectory(java.nio.file.Path,java.nio.file.attribute.BasicFileAttributes), \
                a.b.X.$0.preVisitDirectory(java.nio.file.Path,java.nio.file.attribute.BasicFileAttributes):0:p, \
                java.nio.file.FileVisitResult.CONTINUE, list\
                """, vdLast.knownVariableNamesToString());
        VariableInfo viList = vdLast.variableInfo("list");
        assertTrue(viList.isModified());

        assertTrue(preVisitDirectory.isModifying());

    }

    @Language("java")
    private static final String INPUT5 = """
            package a.b;
            public class X {
                static class M { int i; M(int i) { this.i = i; } void setI(int i) { this.i = i; } }
                static Go callGo(int i) {
                    M m = new M(i);
                    return new Go(m);
                }
                static class Go {
                    private M m;
                    Go(M m) {
                        this.m = m;
                    }
                    void inc() {
                        this.m.i++;
                    }
                }
                static class Go2 {
                    private M m;
                    Go2(M m) {
                        this.m = m;
                    }
                    int get() {
                        return this.m.i;
                    }
                }
            }
            """;

    @DisplayName("does the modification travel via the field?")
    @Test
    public void test5() {
        TypeInfo X = javaInspector.parse(INPUT5);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        {
            TypeInfo go = X.findSubType("Go");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertFalse(p0.isUnmodified());
        }
        {
            TypeInfo go = X.findSubType("Go2");
            MethodInfo constructor = go.findConstructor(1);
            ParameterInfo p0 = constructor.parameters().getFirst();
            assertTrue(p0.isUnmodified());
        }
    }

}
