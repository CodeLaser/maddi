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

package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.link.CommonTest;
import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.modification.prepwork.variable.VariableInfo;
import io.codelaser.maddi.modification.prepwork.variable.impl.VariableDataImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shadow clone bench try_pure_compiles getPluginImageURL (9 clones): a reflective call inside an if inside a try,
 * then the same shape inside a BARE BLOCK. The #84 block merge first read every variable as modified at the block
 * (the merge started from an evaluation stage the block never had): +9 reverse divergences on 2026-09-28. A bare
 * block goes through the statement path like any other statement; the block's links survive to the method.
 */
public class TestBareBlockStatement extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.lang.reflect.Constructor;
            import java.lang.reflect.Method;
            import java.net.URL;
            class X {
                public static URL getPluginImageURL(final Object plugin, final String name) throws Exception {
                    try {
                        Class<?> bundleClass = Class.forName("org.osgi.framework.Bundle");
                        Class<?> bundleContextClass = Class.forName("org.osgi.framework.BundleContext");
                        if (bundleContextClass.isAssignableFrom(plugin.getClass())) {
                            Method getBundleMethod = bundleContextClass.getMethod("getBundle", new Class[0]);
                            Object bundle = getBundleMethod.invoke(plugin, new Object[0]);
                            Class<?> ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
                            Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path");
                            Constructor<?> pathConstructor = pathClass.getConstructor(new Class[] { String.class });
                            Object path = pathConstructor.newInstance(new Object[] { name });
                            Class<?> platformClass = Class.forName("org.eclipse.core.runtime.Platform");
                            Method findMethod = platformClass.getMethod("find", new Class[] { bundleClass, ipathClass });
                            return (URL) findMethod.invoke(null, new Object[] { bundle, path });
                        }
                    } catch (Throwable e) {
                    }
                    {
                        Class<?> pluginClass = Class.forName("org.eclipse.core.runtime.Plugin");
                        if (pluginClass.isAssignableFrom(plugin.getClass())) {
                            Class<?> ipathClass = Class.forName("org.eclipse.core.runtime.IPath");
                            Class<?> pathClass = Class.forName("org.eclipse.core.runtime.Path");
                            Constructor<?> pathConstructor = pathClass.getConstructor(new Class[] { String.class });
                            Object path = pathConstructor.newInstance(new Object[] { name });
                            Method findMethod = pluginClass.getMethod("find", new Class[] { ipathClass });
                            return (URL) findMethod.invoke(plugin, new Object[] { path });
                        }
                    }
                    return null;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, LinkComputer.Options.TEST);
        tlc.doPrimaryType(X);
        MethodInfo m = X.findUniqueMethod("getPluginImageURL", 2);
        MethodLinkedVariables mlv = m.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(mlv);
        assertEquals("[TryStatementImpl@0, BlockImpl@1, ReturnStatementImpl@2]",
                m.methodBody().statements().stream().map(s -> s.getClass().getSimpleName() + "@" + s.source().index()).toList().toString());
        // the return value of findMethod.invoke(plugin, ...) inside the bare block is linked to the parameter
        assertEquals("[0:plugin.§$∩$_ce18.§$, -] --> getPluginImageURL←$_ce33,getPluginImageURL.§$≺$_ce33,getPluginImageURL.§$←$_ce18.§$,getPluginImageURL.§$←0:plugin.§$",
                mlv.toString());
        ParameterInfo plugin = m.parameters().getFirst();
        assertTrue(plugin.analysis().getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue());
        ParameterInfo name = m.parameters().get(1);
        assertTrue(name.analysis().getOrDefault(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.FALSE).isTrue());
        for (Statement st : m.methodBody().statements()) {
            VariableData vd = VariableDataImpl.of(st);
            assertNotNull(vd);
            for (VariableInfo vi : vd.variableInfoIterable()) {
                if (vi.variable() instanceof ParameterInfo) {
                    assertTrue(vi.isUnmodified(), st.source().index() + " " + vi.variable());
                }
            }
        }
    }
}
