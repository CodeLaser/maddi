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
package io.codelaser.maddi.ide.plugin.kotlin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;

import java.util.List;

/**
 * Kotlin's PSI mapped onto maddi's element kinds, for the surfaces that anchor on a declaration's name.
 * <p>
 * The anchor is the name token itself: Kotlin has no {@code PsiIdentifier}, its declarations own an
 * {@code IDENTIFIER} leaf ({@code getNameIdentifier()}). Matching the leaf to an analysed element is then the
 * shared range-containment rule, which is why the kind is all this class decides.
 * <p>
 * ⚠ Only the shapes whose meaning is plain are mapped. A declaration with no name of its own (a primary
 * constructor, {@code companion object}, an {@code init} block) has no leaf to anchor on and gets nothing yet;
 * a constructor {@code val} parameter is shown as the PARAMETER it is, not as the property it also declares.
 */
public final class KotlinDeclarations {
    private KotlinDeclarations() {
    }

    /**
     * The kinds to look for at this leaf, in order of preference. A property is its backing FIELD, and when it
     * has none (a custom getter, {@code val sum get() = x + y}) the analysed element is its accessor, a METHOD
     * over the property's range: without the fallback that verdict had nowhere to go.
     */
    public static List<String> kindsOf(PsiElement leaf) {
        String kind = kindOf(leaf);
        if (kind == null) return List.of();
        return "FIELD".equals(kind) ? List.of("FIELD", "METHOD") : List.of(kind);
    }

    public static @Nullable String kindOf(PsiElement leaf) {
        IElementType type = leaf.getNode() == null ? null : leaf.getNode().getElementType();
        if (type != KtTokens.IDENTIFIER) return null;
        if (!(leaf.getParent() instanceof KtNamedDeclaration declaration)
            || declaration.getNameIdentifier() != leaf) {
            return null;
        }
        if (declaration instanceof KtClassOrObject) return "TYPE";
        if (declaration instanceof KtNamedFunction) return "METHOD";
        if (declaration instanceof KtProperty) return "FIELD";   // no backing field: see kindsOf
        if (declaration instanceof KtParameter) return "PARAMETER";
        return null;
    }
}
