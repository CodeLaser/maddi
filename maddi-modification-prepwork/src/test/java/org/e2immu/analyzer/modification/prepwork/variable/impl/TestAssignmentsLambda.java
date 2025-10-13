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

package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignmentsLambda extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            import java.net.URI;
            import java.nio.file.FileSystem;
            import java.nio.file.FileSystems;
            import java.nio.file.Files;
            import java.nio.file.Path;
            import java.nio.file.Paths;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.stream.Stream;
            import java.util.zip.ZipEntry;
            import java.util.zip.ZipOutputStream;
            
            public class X {
              public static void method(String path, String modules) throws Throwable {
                FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
                try (ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(Paths.get(path)));
                    Stream<Path> stream = Files.walk(fs.getPath("/"))) {
                  stream.forEach(
                      p -> {
                        if (!Files.isRegularFile(p)) {
                          return;
                        }
                        try {
                          byte[] data = Files.readAllBytes(p);
                          List<String> list = new ArrayList<>();
                          p.iterator().forEachRemaining(p2 -> list.add(p2.toString()));
                          assert list.remove(0).equals(modules);
                          if (!list.get(list.size() - 1).equals("module-info.class")) {
                            list.remove(0);
                          }
                          list.remove(0);
                          String outPath = String.join("/", list);
                          if (!outPath.endsWith("module-info.class")) {
                            ZipEntry ze = new ZipEntry(outPath);
                            zipStream.putNextEntry(ze);
                            zipStream.write(data);
                          }
                        } catch (Throwable t) {
                          throw new RuntimeException(t);
                        }
                      });
                }
              }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);
        assertFalse(vdMethod.isKnown("zipStream"));
        Statement forEach = method.methodBody().statements().get(1).block().statements().get(0);
        assertEquals("1.0.0", forEach.source().index());
        VariableData vd = VariableDataImpl.of(forEach);
        VariableInfo viZipStream = vd.variableInfo("zipStream");
        assertEquals("D:1+0, A:[1+0]", viZipStream.assignments().toString());
        assertEquals("1.0.0", viZipStream.reads().toString());

        if (forEach.expression() instanceof MethodCall mc && mc.parameterExpressions().get(0) instanceof Lambda lambda) {
            MethodInfo inLambda = lambda.methodInfo();
            Statement s1 = inLambda.methodBody().statements().get(1);
            VariableData vd1 = VariableDataImpl.of(s1);
            VariableInfo viZipStream1 = vd1.variableInfo("zipStream");
            assertEquals("D:+, A:[]", viZipStream1.assignments().toString());
            assertEquals("1.0.7.0.1, 1.0.7.0.2", viZipStream1.reads().toString());
        } else fail();
    }
}
