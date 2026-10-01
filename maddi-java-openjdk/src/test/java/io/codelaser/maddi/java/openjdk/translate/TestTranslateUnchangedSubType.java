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
package io.codelaser.maddi.java.openjdk.translate;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A translation that changes a type re-creates it, and so must re-create EVERY subtype: a subtype is owned by its
 * enclosing type. A subtype the translation leaves untouched -- a member-less nested interface, which has nothing
 * that can change -- came back as itself and was added to the new type while it still named the old one as its
 * enclosing type. That holds for its own subtypes too, at any depth. Found on guava's {@code Suppliers}
 * ({@code interface SupplierFunction<T> extends Function<...> {}}) and {@code MapMakerInternalMap}
 * ({@code interface StrongValueEntry<K, V, E> extends InternalEntry<K, V, E> {}}), renamed by a translation of the whole type.
 */
public class TestTranslateUnchangedSubType extends CommonTest {

    @Language("java")
    public static final String INPUT = """
            package a.b;
            class X {
                String k() {
                    return "k";
                }
                static class Changed {
                    String k() {
                        return "k";
                    }
                }
                interface Empty<T> extends Comparable<T> {
                    interface Deeper<U> {
                    }
                }
            }
            """;

    @DisplayName("every subtype of a re-created type is enclosed by the re-created type, at any depth")
    @Test
    public void test() {
        TypeInfo X = scan("a.b.X", INPUT);
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .setClearAnalysis(true)
                .put(runtime.newStringConstant("k"), runtime.newStringConstant("j"))
                .build();
        TypeInfo translated = X.translate(tm).getFirst();
        assertNotSame(X, translated);
        assertEquals(2, translated.subTypes().size());
        for (TypeInfo sub : translated.subTypes()) {
            assertSame(translated, sub.compilationUnitOrEnclosingType().getRight(), sub.simpleName());
            for (TypeInfo subSub : sub.subTypes()) {
                assertSame(sub, subSub.compilationUnitOrEnclosingType().getRight(), subSub.simpleName());
            }
        }
        TypeInfo empty = translated.findSubType("Empty");
        assertNotSame(X.findSubType("Empty"), empty);
        assertSame(empty, empty.typeParameters().getFirst().getOwner().getLeft());
        assertEquals(1, empty.interfacesImplemented().size());
        TypeInfo deeper = empty.findSubType("Deeper");
        assertSame(deeper, deeper.typeParameters().getFirst().getOwner().getLeft());
    }

    // control: re-creating the subtypes happens only when their enclosing type is re-created. (A type with
    // methods is re-created by any translation, so only a member-less one can show the identity.)
    @Language("java")
    public static final String MEMBERLESS = """
            package a.b;
            interface Y {
                interface Empty<T> extends Comparable<T> {
                    interface Deeper<U> {
                    }
                }
            }
            """;

    @DisplayName("a translation that changes nothing still returns the type itself")
    @Test
    public void noChange() {
        TypeInfo Y = scan("a.b.Y", MEMBERLESS);
        TranslationMap tm = runtime.newTranslationMapBuilder()
                .put(runtime.newStringConstant("absent"), runtime.newStringConstant("j"))
                .build();
        assertSame(Y, Y.translate(tm).getFirst());
    }
}
