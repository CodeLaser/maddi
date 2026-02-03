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

package org.e2immu.analyzer.modification.link.impl;


import org.e2immu.analyzer.modification.link.CommonTest;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSystemArrayCopy extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.*;
            import java.net.*; // unneeded
            public class X {
                private byte[] readStream(InputStream fin) throws IOException {
                    byte[] buf = new byte[4096];
                    int size = 0;
                    int len = 0;
            
                    do {
                        size += len;
            
                        if(buf.length - size <= 0) {
                            byte[] newbuf = new byte[buf.length * 2];
                            System.arraycopy(buf, 0, newbuf, 0, size);
                            buf = newbuf;
                        }
            
                        len = fin.read(buf, size, buf.length - size);
                    } while(len >= 0);
            
                    byte[] result = new byte[size];
                    System.arraycopy(buf, 0, result, 0, size);
                    return result;
                }
            }
            """;

    // See TestJavaLang.systemArraycopy()
    @DisplayName("crossLink System.arraycopy()")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        MethodInfo readStream = X.findUniqueMethod("readStream", 1);
        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlv = tlc.doMethod(readStream);
        assertEquals("[-] --> -", mlv.toString());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class X<T> {
                public static Object[] copy(Object[] in) {
                    int size = in.length;
                    Object[] out = new Object[size];
                    System.arraycopy(in, 0, out, 0, size);
                    return out;
                }
                T[] ts;
                public void go(T[] input) {
                    ts = copy(input);
                }
            }
            """;

    @DisplayName("crossLink System.arraycopy(), objects")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build());
        analyzer.doPrimaryType(X);

        TypeInfo system = javaInspector.compiledTypesManager().getOrLoad(System.class);
        MethodInfo arrayCopy = system.findUniqueMethod("arraycopy", 5);
        ParameterInfo in = arrayCopy.parameters().getFirst();
        Value.Independent independentIn = in.analysis().getOrNull(INDEPENDENT_PARAMETER, ValueImpl.IndependentImpl.class);
        assertEquals("@Independent(hc=true, hcParameters={2})", independentIn.toString());

        LinkComputer tlc = new LinkComputerImpl(javaInspector);
        MethodLinkedVariables mlvArrayCopy = arrayCopy.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(arrayCopy));
        assertEquals("[-, -, 2:object1.§$←0:object.§$, -, -] --> -", mlvArrayCopy.toString());

        MethodInfo copy = X.findUniqueMethod("copy", 1);
        MethodLinkedVariables mlvCopy = copy.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(copy));

        VariableData vd2 = VariableDataImpl.of(copy.methodBody().statements().get(2));
        VariableInfo out2 = vd2.variableInfo("out");
        assertEquals("out.§$←0:in.§$", out2.linkedVariables().toString());

        assertEquals("[-] --> copy.§$←0:in.§$", mlvCopy.toString());

        MethodInfo go = X.findUniqueMethod("go", 1);
        MethodLinkedVariables mlvGo = go.analysis().getOrCreate(METHOD_LINKS, () -> tlc.doMethod(go));
        // this is not quite what we want, but it is what it is. Blame JDK 1.0
        assertEquals("[0:input.§$→this*.ts.§$] --> -", mlvGo.toString());
    }
}
