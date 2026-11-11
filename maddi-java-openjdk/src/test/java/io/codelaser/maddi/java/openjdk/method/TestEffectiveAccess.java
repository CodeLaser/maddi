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
package io.codelaser.maddi.java.openjdk.method;

import java.util.Map;

import io.codelaser.maddi.cst.api.info.Access;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Info#effectiveAccess()}: the narrowest of an element's own access and of everything it is declared in,
 * against {@link Info#access()}. Where the two differ (2026-11-11): a method's {@code access()} is its own, and so is
 * an interface field's; a class's field is narrowed by its owner already, and a type by its enclosing types. The
 * cases are those of refactor-base's {@code EffectiveAccess}, which this replaces.
 */
public class TestEffectiveAccess extends CommonTest {

    @Language("java")
    private static final String SITE = """
            package a;
            public class Site {
                interface DtoFactory { Object create(); }
                public static class Open { public void run() { } public int n; }
                private static class Hidden { public void run() { } public int n; }
                protected static class Kin { public void go() { } }
                public void make() { }
                Runnable later() {
                    class Local { public void run() { } }
                    return new Runnable() { public void run() { } };
                }
            }
            """;

    @Language("java")
    private static final String API = """
            package a;
            public interface Api {
                class Impl { }
                int LIMIT = 3;
                void call();
            }
            """;

    @Language("java")
    private static final String INNER = """
            package a;
            interface Inner {
                int LIMIT = 4;
                class Nested { public void go() { } }
            }
            """;

    @Language("java")
    private static final String COLOR = """
            package a;
            public enum Color { RED; Color() { } }
            """;

    private static String words(Access own, Access effective) {
        return word(own) + " -> " + word(effective);
    }

    private static String word(Access a) {
        if (a == null) return "null";
        return a.isPublic() ? "public" : a.isProtected() ? "protected" : a.isPackage() ? "package"
                : a.isPrivate() ? "private" : a.isInternal() ? "internal" : "?";
    }

    private static String of(Info info) {
        return words(info.access(), info.effectiveAccess());
    }

    private final List<String> wrong = new ArrayList<>();

    private void check(String expected, String actual, int line) {
        if (!expected.equals(actual)) wrong.add("line " + line + ": expected " + expected + ", was " + actual);
    }

    @Test
    public void effectiveAccess() {
        Map<String, TypeInfo> types = scan(false, "a.Site", SITE, "a.Api", API, "a.Inner", INNER, "a.Color", COLOR);
        TypeInfo site = types.get("a.Site");
        TypeInfo api = types.get("a.Api");
        TypeInfo inner = types.get("a.Inner");
        TypeInfo color = types.get("a.Color");

        // types: access() already narrows by the enclosing types; effectiveAccess() agrees
        check("public -> public", of(site), 109);
        check("package -> package", of(site.findSubType("DtoFactory")), 110);
        check("public -> public", of(site.findSubType("Open")), 111);
        check("private -> private", of(site.findSubType("Hidden")), 112);
        check("protected -> protected", of(site.findSubType("Kin")), 113);
        check("public -> public", of(api.findSubType("Impl")), 114);
        check("package -> package", of(inner), 115);
        check("package -> package", of(inner.findSubType("Nested")), 116);

        // members: their own access, narrowed by the type's
        check("public -> public", of(site.findUniqueMethod("make", 0)), 119);
        check("public -> public", of(site.findSubType("Open").findUniqueMethod("run", 0)), 120);
        check("public -> private", of(site.findSubType("Hidden").findUniqueMethod("run", 0)), 121);
        // access() of a class's field is narrowed by its owner already; of an interface's field it is not (below)
        check("private -> private", of(site.findSubType("Hidden").getFieldByName("n", true)), 122);
        check("public -> protected", of(site.findSubType("Kin").findUniqueMethod("go", 0)), 123);
        check("public -> package", of(inner.findSubType("Nested").findUniqueMethod("go", 0)), 124);
        check("public -> public", of(api.findUniqueMethod("call", 0)), 125);
        check("public -> public", of(api.getFieldByName("LIMIT", true)), 126);
        check("public -> package", of(inner.getFieldByName("LIMIT", true)), 127);
        check("public -> public", of(color.getFieldByName("RED", true)), 128);
        check("private -> private", of(color.findConstructor(0)), 129);

        // a local and an anonymous class: nothing outside the method names them
        MethodInfo later = site.findUniqueMethod("later", 0);
        TypeInfo local = site.recursiveSubTypeStreamIncludingLocal()
                .filter(t -> t.enclosingMethod() == later && !t.isAnonymous()).findFirst().orElseThrow();
        TypeInfo anonymous = site.recursiveSubTypeStreamIncludingLocal()
                .filter(TypeInfo::isAnonymous).findFirst().orElseThrow();
        check("private -> private", of(local), 137);
        check("private -> private", of(anonymous), 138);
        check("public -> private", of(local.findUniqueMethod("run", 0)), 139);
        check("public -> private", of(anonymous.findUniqueMethod("run", 0)), 140);
        assertEquals(List.of(), wrong);
    }
}
