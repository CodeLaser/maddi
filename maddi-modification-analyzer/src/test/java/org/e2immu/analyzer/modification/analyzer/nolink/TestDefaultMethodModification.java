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

package org.e2immu.analyzer.modification.analyzer.nolink;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modification of an implementation's field that reaches the field only through an inherited interface <em>default
 * method</em> (which mutates via an abstract accessor the implementation overrides to return the field).
 * <p>
 * The positive control -- the identical body written as a concrete method in the implementation -- is detected. The
 * inherited-default-method variant is NOT (a false negative, {@code @Disabled} below): the default method's body is
 * analyzed in the interface's context, where the abstract accessor returns an unknown value, so its modification never
 * links to each implementation's concrete field. Impact is narrow: the type-level immutability/independence/container
 * classifications are unaffected here (the accessor already exposes the field, forcing @FinalFields); only the
 * field-level UNMODIFIED_FIELD is unsound for this pattern. Fixing it means re-analyzing inherited default methods per
 * implementation.
 */
public class TestDefaultMethodModification extends CommonTest {

    @Language("java") private static final String CONCRETE = """
            package a.b;
            import java.util.List;
            class X {
                interface Buffer { List<String> items(); void add(String s); }
                static class Impl implements Buffer {
                    private final List<String> list = new java.util.ArrayList<>();
                    public List<String> items() { return list; }
                    public void add(String s) { items().add(s); }
                }
            }
            """;

    @DisplayName("control: modification through a concrete method (via an accessor) is detected")
    @Test
    public void concreteMethod() {
        TypeInfo X = javaInspector.parse("a.b.X", CONCRETE);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        assertTrue(X.findSubType("Impl").getFieldByName("list", true).isModified());
    }

    @Language("java") private static final String INHERITED_DEFAULT = """
            package a.b;
            import java.util.List;
            class X {
                interface Buffer {
                    List<String> items();
                    default void add(String s) { items().add(s); }
                }
                static class Impl implements Buffer {
                    private final List<String> list = new java.util.ArrayList<>();
                    public List<String> items() { return list; }
                }
            }
            """;

    @DisplayName("modification through an inherited default method must reach the implementation's field")
    @Disabled("Known false negative: modification via an inherited interface default method (which mutates through an "
            + "abstract accessor overridden to return the field) is not propagated to the implementation's field. "
            + "Enable once inherited default methods are re-analyzed per implementation.")
    @Test
    public void inheritedDefaultMethod() {
        TypeInfo X = javaInspector.parse("a.b.X", INHERITED_DEFAULT);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        assertTrue(X.findSubType("Impl").getFieldByName("list", true).isModified(),
                "Impl.list is modifiable via the inherited add()");
    }
}
