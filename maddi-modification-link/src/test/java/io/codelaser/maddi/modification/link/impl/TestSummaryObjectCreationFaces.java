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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.link.CommonTest;

import io.codelaser.maddi.modification.link.LinkComputer;
import io.codelaser.maddi.modification.prepwork.PrepAnalyzer;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * #91: a method's summary must not carry FACES of the objects it creates ('oc:15-31.§$s', 'oc:26-20.first',
 * 'oc:39-16.size'). Such an object is anonymous to every caller: the caller reaches it only through the return
 * value or a parameter's fields, and those paths are in the summary already ('fillAfter.keys ← oc:15-31' plus
 * 'fillAfter.keys.§$s ~ 0:keys.§$s' say everything 'fillAfter ≻ oc:15-31.§$s' said). On timefold's selector
 * factories the faces were 55 % of all summary links, re-imported and re-exported by every caller up the chain
 * (a 3,000-link summary, 24 methods at the work ceiling per pass); dropping them at export took a pass from 653M
 * to 282M work with no verdict moving on the clone bench, the shadow bench or detekt. The bare marker stays:
 * 'fillAfter ← oc:15-20' is fresh-object provenance. Gate KEEPOCFACES restores the old export (A/B only).
 * <p>
 * PRODUCTION options: object creations are tracked (TEST switches them off, so it never sees a marker).
 */
public class TestSummaryObjectCreationFaces extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Supplier;
            class X {
                static class Sorter { final List<String> keys; Supplier<String> first; Sorter(List<String> keys) { this.keys = keys; } }
                static class Selector {
                    final Sorter sorter; final List<String> names;
                    Selector(Sorter sorter, List<String> names) { this.sorter = sorter; this.names = names; }
                }
                static class Holder { Selector selector; int size; Holder(Selector selector, int size) { this.selector = selector; this.size = size; } }

                // A: the created object's collection is filled from the parameter after construction
                static Sorter fillAfter(List<String> keys) {
                    Sorter s = new Sorter(new ArrayList<>());
                    s.keys.addAll(keys);
                    return s;
                }
                // B: the created object is stored into a parameter's field (no return)
                static void storeInto(List<String> keys, Holder h) {
                    Sorter s = new Sorter(keys);
                    h.selector = new Selector(s, List.of());
                }
                // C: a lambda over the parameter is stored in the created object's field
                static Sorter withSupplier(List<String> keys) {
                    Sorter s = new Sorter(List.of());
                    s.first = () -> keys.get(0);
                    return s;
                }
                // D: the created object is returned wrapped one level deeper, its field assigned from the parameter
                static Holder wrap(List<String> keys, List<String> names, boolean sorted) {
                    Sorter sorter;
                    if (sorted) {
                        sorter = new Sorter(keys);
                    } else {
                        sorter = new Sorter(List.of());
                    }
                    Selector selector = new Selector(sorter, names);
                    return new Holder(selector, keys.size());
                }
                // E: the caller of D
                static Holder top(List<String> keys, List<String> names) {
                    Holder h = wrap(keys, names, true);
                    h.size = 2;
                    return h;
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        new PrepAnalyzer(runtime, new PrepAnalyzer.Options.Builder().build()).doPrimaryType(X);
        LinkComputer tlc = new LinkComputerImpl(javaInspector, LinkComputer.Options.PRODUCTION);
        tlc.doPrimaryType(X);
        boolean pruned = !Gate.isSet("KEEPOCFACES");
        for (String name : new String[]{"fillAfter", "storeInto", "withSupplier", "wrap", "top"}) {
            String s = summary(X, name);
            assertFalse(s.contains("$__sv_"), name + " summary carries a shared-variable rep: " + s);
            if (pruned) {
                assertFalse(s.matches("(?s).*oc:\\d+-\\d+\\..*"), name + " summary carries a face of an object-creation marker: " + s);
            }
        }
        if (pruned) {
            // with KEEPOCFACES: [0:keys.§$s~oc:15-31.§$s] --> ..., fillAfter≻oc:15-31.§$s, ..., fillAfter.keys.§m≡oc:15-31.§m
            assertEquals("[-] --> fillAfter.keys←oc:15-31,fillAfter←oc:15-20,fillAfter.keys.§$s~0:keys.§$s", summary(X, "fillAfter"));
            // with KEEPOCFACES: ..., withSupplier.first←Λoc:26-20.first, ...
            assertEquals("[-] --> withSupplier.first←Λ$_fi2,withSupplier←oc:26-20", summary(X, "withSupplier"));
            // with KEEPOCFACES: ..., top.size←oc:39-16.size, ...
            assertEquals("[-, -] --> top.selector≈oc:34-22,top.selector≈oc:36-22,top.selector←oc:38-29,top.size←$_ce2,"
                         + "top.selector.names←1:names,top≈oc:34-22,top≈oc:36-22,top←oc:39-16,top.selector.names.§m≡1:names.§m",
                    summary(X, "top"));
        }
        // the object stored into a parameter's field keeps its path spelled on the parameter, in both modes
        assertEquals("[-, 1:h*.selector←oc:22-22,1:h*.selector.sorter←oc:21-20,1:h*.selector.sorter.keys←0:keys,"
                     + "1:h*.selector.sorter.keys.§m≡0:keys.§m] --> -", summary(X, "storeInto"));
    }

    private static String summary(TypeInfo X, String name) {
        MethodInfo m = X.methods().stream().filter(mi -> mi.name().equals(name)).findFirst().orElseThrow();
        MethodLinkedVariables mlv = m.analysis().getOrNull(MethodLinkedVariablesImpl.METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assertNotNull(mlv);
        return mlv.toString();
    }
}
