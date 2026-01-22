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

package org.e2immu.analyzer.modification.analyzer.io;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.io.LinkCodec;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalyzedPackageFiles;
import org.e2immu.analyzer.modification.prepwork.io.WriteAnalysis;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.util.internal.util.Trie;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestWriteAnalysis3 extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestWriteAnalysis3.class);


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.util.AbstractMap;
            import java.util.Map;
            import java.util.stream.Stream;
            public class Try {
                public interface TryData {
                    Throwable exception();
                    TryData withException(Throwable exception);
                }
                public static class TryDataImpl implements TryData {
                    private final Throwable exception;
                    public TryDataImpl(Throwable exception) { this.exception = exception; }
                    public Throwable exception() { return exception; }
                    public TryData withException(Throwable exception) { return new TryDataImpl(exception); }
                }
            }
            """;

    @Test
    public void test1() throws IOException {
        TypeInfo Try = javaInspector.parse(INPUT1);
        TypeInfo TryDataImpl = Try.findSubType("TryDataImpl");

        List<Info> ao = prepWork(Try);
        analyzer.go(ao);

        MethodInfo exception = TryDataImpl.findUniqueMethod("exception", 0);
        MethodLinkedVariables mlv = exception.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[] --> exception←this.exception", mlv.toString());

        FieldInfo exceptionField = TryDataImpl.getFieldByName("exception", true);
        assertEquals("""
                this.exception←0:exception,this.exception→exception\
                """, exceptionField.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class).toString());

        MethodInfo withException = TryDataImpl.findUniqueMethod("withException", 1);
        MethodLinkedVariables mlvWe = withException.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals("[-] --> withException.exception←0:exception", mlvWe.toString());
        FieldReference fr0 = (FieldReference) mlvWe.ofReturnValue().link(0).from();
        assertEquals("a.b.Try.TryData", fr0.fieldInfo().owner().toString());

        Trie<TypeInfo> typeTrie = new Trie<>();
        typeTrie.add(Try.fullyQualifiedName().split("\\."), Try);
        WriteAnalysis writeAnalysis = new WriteAnalysis(runtime);
        File dest = new File("build/json");
        if (dest.mkdirs()) LOGGER.info("Created {}", dest);
        Codec codec = new LinkCodec(runtime, javaInspector.mainSources()).codec();
        writeAnalysis.write(dest, typeTrie, codec);
        String written = Files.readString(new File(dest, "ABTry.json").toPath());

        LOGGER.info("Encoded: {}", written);

        javaInspector.invalidateAllSources();
        TypeInfo Try1 = javaInspector.parse(INPUT1);
        TypeInfo TryDataImpl1 = Try1.findSubType("TryDataImpl");
        LoadAnalyzedPackageFiles load = new LoadAnalyzedPackageFiles(javaInspector.mainSources());
        load.go(codec, written);

        MethodInfo exception1 = TryDataImpl1.findUniqueMethod("exception", 0);
        MethodLinkedVariables mlv1 = exception1.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertEquals(mlv, mlv1);
    }

    private void testLink4(MethodLinkedVariables mlv) {
        assertEquals("""
                [-, -] --> oneInstance.§ksvs.§ks∋0:x,oneInstance.§ksvs.§vs∋1:y\
                """, mlv.toString());

        FieldReference ks = (FieldReference) mlv.ofReturnValue().link(0).from();
        assertEquals("§ks", ks.fieldInfo().name());
        assertEquals("java.util.AbstractMap.SimpleEntry.KSVS", ks.fieldInfo().owner().fullyQualifiedName());
        assertEquals("Type param K[]", ks.parameterizedType().toString());

        FieldReference ksvs = (FieldReference) ks.scopeVariable();
        assertEquals("§ksvs", ksvs.fieldInfo().name());
        assertEquals("Type java.util.AbstractMap.SimpleEntry.KSVS", ksvs.parameterizedType().toString());
        assertEquals("java.util.Map.Entry", ksvs.fieldInfo().owner().fullyQualifiedName());
        assertSame(VirtualFieldComputer.VIRTUAL_FIELD, ksvs.parameterizedType().typeInfo().typeNature());
    }
}
