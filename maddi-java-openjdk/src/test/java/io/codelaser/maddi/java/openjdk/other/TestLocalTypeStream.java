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

package io.codelaser.maddi.java.openjdk.other;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CodeLaser/maddi#126: the anonymous and local classes of a type's code, which {@code subTypes()} does not hold.
 * A tool walking {@code recursiveSubTypeStream()} never saw the loop inside {@code new HttpResponse() { … }}.
 */
public class TestLocalTypeStream extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Supplier;
            class X {
                Runnable field = new Runnable() { public void run() { } };
                static Object fromInitializer;
                static {
                    fromInitializer = new Object() { };
                }
                Object method() {
                    class Local {
                        Object inLocal() { return new Object() { }; }
                    }
                    Supplier<Object> s = () -> new Object() {
                        Runnable nested = new Runnable() { public void run() { } };
                    };
                    return new Local();
                }
                static class Member {
                    Object m() { return new Object() { }; }
                }
            }
            """;

    private static List<String> names(List<TypeInfo> types) {
        return types.stream().map(ti -> ti.isAnonymous() ? "anonymous " + ti.source().beginLine()
                : ti.simpleName() + " " + ti.source().beginLine()).toList();
    }

    @DisplayName("the anonymous and local classes declared in this type's own code, in source order")
    @Test
    public void direct() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals(List.of("anonymous 4", "anonymous 7", "Local 10", "anonymous 13"),
                names(typeInfo.localTypeStream().toList()));
    }

    @DisplayName("descending into member types and local types, at any depth")
    @Test
    public void recursive() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals(List.of("X 3", "Member 18", "anonymous 19", "anonymous 4", "anonymous 7", "Local 10",
                        "anonymous 11", "anonymous 13", "anonymous 14"),
                names(typeInfo.recursiveSubTypeStreamIncludingLocal().toList()));
        assertEquals(List.of("X 3", "Member 18"), names(typeInfo.recursiveSubTypeStream().toList()));
    }
}
