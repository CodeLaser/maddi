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

package org.e2immu.analyzer.modification.io;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.util.internal.util.Trie;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWriteAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestWriteAnalysis.class);

    private final Runtime runtime = new RuntimeImpl();

    @Language("json")
    private static final String EXPECT = """
            [
            {"name": "Torg.e2immu.C", "data":{"commutableMethods":["p1","p2,p3","p4"],"defaultsAnalyzer":1,"immutableType":3}, "sub":
             {"name": "Mm1(0)", "data":{"defaultsAnalyzer":1}}}
            ]
            """;

    @Test
    public void test() throws IOException {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("org.e2immu").build();
        TypeInfo typeInfo = runtime.newTypeInfo(cu, "C");

        typeInfo.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE);
        typeInfo.analysis().set(PropertyImpl.DEFAULTS_ANALYZER, ValueImpl.BoolImpl.TRUE);
        typeInfo.analysis().set(PropertyImpl.COMMUTABLE_METHODS,
                new ValueImpl.CommutableDataImpl("p1", "p2,p3", "p4"));

        MethodInfo methodInfo = runtime.newMethod(typeInfo, "m1", runtime.methodTypeMethod());
        methodInfo.analysis().set(PropertyImpl.DEFAULTS_ANALYZER, ValueImpl.BoolImpl.TRUE);
        typeInfo.builder().addMethod(methodInfo);

        WriteAnalysis wa = new WriteAnalysis(runtime);
        Trie<TypeInfo> trie = new Trie<>();
        trie.add(new String[]{"org", "e2immu"}, typeInfo);
        File dir = new File("build");
        File targetFile = new File(dir, "OrgE2immu.json");
        if (targetFile.delete()) LOGGER.debug("Deleted {}", targetFile);
        wa.write(dir.getAbsolutePath(), trie);
        String s = Files.readString(targetFile.toPath());

        assertEquals(EXPECT, s);
    }
}
