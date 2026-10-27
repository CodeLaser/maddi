/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
