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

package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_METHOD;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.HCS_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestArrayConstructor extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.util.Collection;
            
            public class B {
            
                @FunctionalInterface
                interface I {
                    boolean implementation();
                }
                void method() {
                    I[] is = new I[0];
                }
            }
            """;

    @DisplayName("array constructor")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        List<Info> analysisOrder = prepAnalyzer.doPrimaryType(B);
        assertEquals("B.<init>(),B.I.<init>(int),B.I.implementation(),B.I,B.method(),B",
                analysisOrder.stream().map(Info::fullyQualifiedName).collect(Collectors.joining(",")));
        TypeInfo I = B.findSubType("I");

        // we cannot find it in I
        assertTrue(I.constructors().stream().noneMatch(mi -> mi.parameters().size() == 1));

        // but it is there
        MethodInfo intConstructor = (MethodInfo) analysisOrder.get(1);
        assertTrue(intConstructor.analysis().haveAnalyzedValueFor(HCS_METHOD));
    }

    @Language("java")
    private static final String INPUT2 = """
            import java.net.InetAddress;
            import java.net.UnknownHostException;
            
            public class X {
            
                public synchronized byte[] getLocalHostAddress() {
                    try {
                        return (InetAddress.getLocalHost().getAddress());
                    } catch (UnknownHostException u) {
                        return (new byte[] { 127, 0, 0, 1 });
                    }
                }
            }
            """;

    @DisplayName("hcs of synthetic array constructor")
    @Test
    public void test2() {
        TypeInfo B = javaInspector.parse(INPUT2);
        List<Info> ao = new PrepAnalyzer(runtime).doPrimaryType(B);
        assertEquals("X.<init>(),X.getLocalHostAddress(),X",
                ao.stream().map(Info::fullyQualifiedName).collect(Collectors.joining(",")));
        MethodInfo methodInfo = (MethodInfo) ao.get(0);
        assertTrue(methodInfo.analysis().haveAnalyzedValueFor(HiddenContentSelector.HCS_METHOD));
    }
}
