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
package io.codelaser.maddi.ide.plugin.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import io.codelaser.maddi.ide.client.AnalysisModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Which analysed element a declaration's name belongs to: of the elements of the wanted kind whose range contains
 * the name, the SMALLEST. A nested type or member sits inside its enclosing type's range, so several same-kind
 * elements can contain one name, and only the innermost is its own.
 * <p>
 * The kinds are tried in order, the first that matches winning: a Kotlin property is its backing field, or, when
 * it has none, its accessor.
 */
public final class MaddiElementMatch {
    private MaddiElementMatch() {
    }

    public static @Nullable AnalysisModel.ElementAnnotation find(List<AnalysisModel.ElementAnnotation> annotations,
                                                                 Document doc, List<String> kinds, int offset,
                                                                 Predicate<AnalysisModel.ElementAnnotation> shown) {
        for (String kind : kinds) {
            AnalysisModel.ElementAnnotation match = null;
            int bestLength = Integer.MAX_VALUE;
            for (AnalysisModel.ElementAnnotation a : annotations) {
                if (!kind.equals(a.kind()) || !shown.test(a)) continue;
                TextRange r = MaddiPositions.range(doc, a.beginLine(), a.beginCol(), a.endLine(), a.endCol());
                if (r != null && r.contains(offset) && r.getLength() < bestLength) {
                    match = a;
                    bestLength = r.getLength();
                }
            }
            if (match != null) return match;
        }
        return null;
    }
}
