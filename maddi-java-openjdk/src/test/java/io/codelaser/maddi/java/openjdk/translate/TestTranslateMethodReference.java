/*
 * e2immu: a static code analyzer for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You
 * should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.java.openjdk.translate;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code TranslationMap} must remap the METHOD of a method reference, not only its scope and types.
 * <p>
 * {@code MethodCallImpl#translate} calls {@code translateMethodInfo}; {@code MethodReferenceImpl#translate}
 * did not, and passed {@code methodInfo} through unchanged — while its own {@code rewire}, three methods
 * below, has always remapped it via {@code infoMap.methodInfo(..)}.
 * <p>
 * Every lever that RELOCATES a member translates bodies through such a map, so this affected all of them
 * equally: extract-companion, move-static-members, split-class, method-object, split-promote. It only
 * became visible where the origin stops declaring the method, because there the result no longer compiles
 * ("invalid method reference"). Found on an OSS corpus 2026-08-13 via extract.extractCompanion.
 */
public class TestTranslateMethodReference extends CommonTest {

    @Language("java")
    private static final String ORIGIN = """
            package a.b;
            import java.util.function.IntUnaryOperator;
            public class Origin {
                public static int helper(int i) { return i + 1; }
                public static IntUnaryOperator ref() { return Origin::helper; }
            }
            """;

    @Language("java")
    private static final String DESTINATION = """
            package a.b;
            public class Destination {
                public static int helper(int i) { return i + 1; }
            }
            """;

    @DisplayName("translating a moved method remaps the method reference to its new owner")
    @Test
    public void methodReferenceFollowsTheMovedMethod() {
        Map<String, TypeInfo> types = scan(false, "a.b.Origin", ORIGIN, "a.b.Destination", DESTINATION);
        TypeInfo origin = types.get("a.b.Origin");
        TypeInfo destination = types.get("a.b.Destination");

        MethodInfo oldHelper = origin.findUniqueMethod("helper", 1);
        MethodInfo newHelper = destination.findUniqueMethod("helper", 1);
        MethodInfo ref = origin.findUniqueMethod("ref", 0);

        assertTrue(ref.methodBody().toString().contains("Origin::helper"),
                "precondition: " + ref.methodBody());

        TranslationMap tm = runtime.newTranslationMapBuilder().put(oldHelper, newHelper).build();
        Statement translated = ref.methodBody().translate(tm).getFirst();

        assertTrue(translated.toString().contains("Destination::helper"),
                "the reference must follow the method to its new owner:\n" + translated);
    }

    @DisplayName("a method reference to a method NOT in the map is left alone")
    @Test
    public void unrelatedMethodReferenceIsUntouched() {
        Map<String, TypeInfo> types = scan(false, "a.b.Origin", ORIGIN, "a.b.Destination", DESTINATION);
        TypeInfo origin = types.get("a.b.Origin");
        TypeInfo destination = types.get("a.b.Destination");

        MethodInfo ref = origin.findUniqueMethod("ref", 0);
        // map something else entirely
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .put(destination.findUniqueMethod("helper", 1), origin.findUniqueMethod("helper", 1))
                .build();
        Statement translated = ref.methodBody().translate(tm).getFirst();

        assertEquals(ref.methodBody().toString(), translated.toString(),
                "nothing in this body is in the map, so it must come back identical");
    }
}
