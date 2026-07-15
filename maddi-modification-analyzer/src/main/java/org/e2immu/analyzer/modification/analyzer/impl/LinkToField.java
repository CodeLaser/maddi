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

package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;

/**
 * One link, seen as "does this reach a field of the instance, and how mutable is what it reaches?".
 * <p>
 * Extracted so that the analyzer deciding independence ({@code TypeModIndyAnalyzerImpl.worstLinkToFields}) and the
 * guard explaining a violation ({@code GuardAnalyzerImpl.blameMethodDependent}) share one definition. They must not
 * drift: the guard's job is to name the very link the analyzer's verdict rests on, so a second, hand-rolled notion of
 * "links to a mutable field" would sooner or later blame something other than the cause.
 */
class LinkToField {

    private LinkToField() {
        // utility
    }

    /**
     * The immutability of what {@code link} reaches, when it reaches a field of the instance; {@code null} when the
     * link does not lead to such a field, or when its nature carries no type to judge.
     */
    static Value.Immutable immutableOfLinkedField(Link link, AnalysisHelper analysisHelper) {
        Variable primaryTo = Util.firstRealVariable(link.to());
        if (!(primaryTo instanceof FieldReference fr) || !fr.scopeIsRecursivelyThis()) return null;
        ParameterizedType type = linkedType(link, primaryTo);
        return type == null ? null : analysisHelper.typeImmutable(type);
    }

    private static ParameterizedType linkedType(Link link, Variable primaryTo) {
        if (primaryTo == link.to() && (
                link.linkNature().equals(LinkNatureImpl.IS_ASSIGNED_TO)
                || link.linkNature().equals(LinkNatureImpl.IS_ASSIGNED_FROM))) {
            // this.set ← 0:set, TestFieldAnalyzer,1,2;
            return primaryTo.parameterizedType();
        }
        if (link.linkNature().equals(LinkNatureImpl.SHARES_ELEMENTS)
            || link.linkNature().equals(LinkNatureImpl.IS_SUPERSET_OF)) {
            // 0:set.§cs⊇this.set.§cs, TestFieldAnalyzer,3
            return link.to().parameterizedType().copyWithoutArrays();
        }
        if (link.linkNature().equals(LinkNatureImpl.CONTAINS_AS_MEMBER)) {
            // 0:element ∋ this.set.§cs
            return link.to().parameterizedType();
        }
        if (link.linkNature().equals(LinkNatureImpl.IS_ELEMENT_OF)) {
            //getFile∈this._mruFileList.§es
            return link.from().parameterizedType();
        }
        return null;
    }

    /**
     * The field the first dependence-causing link of {@code links} reaches: the one that makes
     * {@code worstLinkToFields} return DEPENDENT, i.e. the first link to a field of the instance whose reached type is
     * mutable. {@code null} when no link is that link.
     */
    static FieldReference firstDependentLinkToField(Links links, AnalysisHelper analysisHelper) {
        for (Link link : links) {
            Value.Immutable immutable = immutableOfLinkedField(link, analysisHelper);
            if (immutable != null && immutable.isMutable()) {
                return (FieldReference) Util.firstRealVariable(link.to());
            }
        }
        return null;
    }
}
