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

package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestModifiedParameterAnon extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
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
                                    return FileVisitResult.CONTINUE;
                                }
            
                            });
                    return list;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
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
}
