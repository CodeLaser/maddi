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

package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ⛔ Found by the Kotlin front end's verdict test: the SAME modifying call, written once inside an
 * {@code if} statement's arm and once inside a conditional EXPRESSION's arm, gave two different answers
 * about the parameter it modifies. The language has nothing to do with it — this is the Java engine.
 */
public class TestModificationInConditionalExpression extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            public class X {
                static class Box {
                    private final List<String> items = new ArrayList<>();
                    int addAndSize(String s) { items.add(s); return items.size(); }
                    String addAndEcho(String s) { items.add(s); return s; }
                    int size() { return items.size(); }
                }
                static int viaIf(Box b, Box c, String t) {
                    int v;
                    if (b == null) { v = c.addAndSize(t); } else { v = b.size(); }
                    return v;
                }
                static int viaTernary(Box b, Box c, String t) {
                    return b == null ? c.addAndSize(t) : b.size();
                }
                static int unconditional(Box b, Box c, String t) {
                    return c.addAndSize(t) + b.size();
                }
                static int inCondition(Box b, Box c, String t) {
                    return c.addAndSize(t) > 0 ? b.size() : 0;
                }
                static int bothArms(Box b, Box c, String t) {
                    return b == null ? c.addAndSize(t) : c.size();
                }
                static int ternaryInsideAnArgument(Box b, Box c, String t) {
                    return b.addAndSize(b == null ? t : c.addAndEcho(t));
                }
                static int ternaryAssignedToALocal(Box b, Box c, String t) {
                    int v = b == null ? c.addAndSize(t) : b.size();
                    return v;
                }
                static int switchArm(Box b, Box c, String t) {
                    return switch (b == null ? 0 : 1) { case 0 -> c.addAndSize(t); default -> b.size(); };
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        // Every one of these writes `c.addAndSize(t)` exactly once, so `c` is modified in every one of
        // them. Before the fix only the first two said so: any conditional expression anywhere in the
        // expression discarded the modification record of everything inside it — including its own CONDITION,
        // which is not conditional at all.
        for (String name : List.of("unconditional", "viaIf", "viaTernary", "inCondition", "bothArms",
                "ternaryInsideAnArgument", "ternaryAssignedToALocal", "switchArm")) {
            assertTrue(second(X, name).isModified(),
                    name + ": `c.addAndSize(t)` modifies `c`, wherever in the expression it stands");
        }
    }

    private static ParameterInfo second(TypeInfo X, String name) {
        MethodInfo m = X.findUniqueMethod(name, 3);
        return m.parameters().get(1);
    }
}
