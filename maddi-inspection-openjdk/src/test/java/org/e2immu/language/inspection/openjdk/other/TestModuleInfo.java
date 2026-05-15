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

package org.e2immu.language.inspection.openjdk.other;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestModuleInfo extends CommonTest {
    private static final String SCAN = """
            package maddi.openjdk;
            public class Scan {
            }
            """;
    private static final String SUPPORT = """
            package maddi.support;
            public class Support {
            }
            """;
    private static final String ABC = """
            package a.b;
            public class C {
            }
            """;
    private static final String DD = """
            package d;
            public class D {
            }
            """;
    private static final String CDD = """
            package c.d;
            public class D {
            }
            """;
    private static final String CDE = """
            package c.d;
            public class E {
            }
            """;
    private static final String CDF = """
            package c.d;
            public class F {
            }
            """;

    @Language("java")
    private static final String MODULE_INFO = """
            module maddi.openjdk {
                requires jdk.net;
                requires transitive java.instrument;
                requires static java.logging;
                requires transitive static java.compiler;
                requires java.xml;
            
                exports maddi.openjdk;
                exports a.b to java.management;
            
                /*we must open*/
                opens a.b to java.datatransfer;
            
                uses a.b.C;
                // usesComment
                uses d.D;
            
                provides a.b.C with c.d.E;
                provides c.d.D with c.d.F;
            }
            """;

    @Test
    public void test0() {
        Map<String, String> map = new HashMap<>();
        map.put("a.b.C", ABC);
        map.put("d.D", DD);
        map.put("c.d.D", CDD);
        map.put("c.d.E", CDE);
        map.put("c.d.F", CDF);
        map.put("maddi.openjdk.Scan", SCAN);
        map.put("maddi.support.Support", SUPPORT);
        map.put("module-info", MODULE_INFO);
        List<Info> list = scan(true, map, List.of());
        ModuleInfo moduleInfo = list.stream().filter(i -> i instanceof ModuleInfo)
                .map(i -> (ModuleInfo) i).findFirst().orElseThrow();
        List<ModuleInfo.Requires> requires = moduleInfo.requires();
        assertEquals(5, requires.size());
        assertTrue(requires.get(1).isTransitive());
        assertFalse(requires.get(1).isStatic());
        assertTrue(requires.get(2).isStatic());
        assertFalse(requires.get(2).isTransitive());
        assertEquals("java.compiler", requires.get(3).name());
        assertTrue(requires.get(3).isStatic());
        assertTrue(requires.get(3).isTransitive());

        List<ModuleInfo.Exports> exports = moduleInfo.exports();
        assertEquals(2, exports.size());
        assertEquals("maddi.openjdk", exports.getFirst().packageName());
        assertNull(exports.getFirst().toPackageNameOrNull());
        assertEquals("java.management", exports.getLast().toPackageNameOrNull());

        List<ModuleInfo.Opens> opens = moduleInfo.opens();
        assertEquals(1, opens.size());
        ModuleInfo.Opens o0 = opens.getFirst();
        assertEquals("a.b", o0.packageName());
        assertEquals("java.datatransfer", o0.toPackageNameOrNull());
        assertEquals("12-11:12-13", o0.source().detailedSources().detail(o0.packageName()).compact2());
        assertEquals("12-18:12-34", o0.source().detailedSources().detail(o0.toPackageNameOrNull()).compact2());
        assertEquals("we must open", o0.comments().getFirst().comment());

        List<ModuleInfo.Uses> uses = moduleInfo.uses();
        assertEquals(2, uses.size());
        assertTrue(uses.getFirst().comments().isEmpty());
        assertEquals(" usesComment", uses.getLast().comments().getFirst().comment());

        List<ModuleInfo.Provides> provides = moduleInfo.provides();
        assertEquals(2, provides.size());
        ModuleInfo.Provides p0 = provides.getFirst();
        assertEquals("a.b.C", p0.api());
        assertEquals("c.d.E", p0.implementation());

        assertFalse(moduleInfo.open());
    }
}
