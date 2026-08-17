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

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

/**
 * What a field HOLDS, as opposed to what its declared type says it may hold.
 * <p>
 * A {@code List<String>} field is DEPENDENT by declared type: handing it out shares a mutable object. But if the
 * object it actually holds is immutable — the defensive copy the constructor stored — then handing it out shares
 * nothing, and the field transmits no dependence. That "dynamic type" information lives in
 * {@link PropertyImpl#IMMUTABLE_FIELD}.
 * <p>
 * Extracted because three separate places grade a field by its declared type and must give the same answer:
 * {@code FieldAnalyzerImpl.computeIndependent} (the field's own independence),
 * {@code TypeModIndyAnalyzerImpl.handleGetterSetter} (a getter's independence), and
 * {@link LinkToField#immutableOfLinkedField} (a return value's link to a field, which the guard also uses to
 * explain a violation). If they drifted, a type could be graded independent while the accessor handing out its
 * field was graded dependent, or the guard would blame a link the verdict did not rest on.
 *
 * <h2>Whole object only</h2>
 * {@code @Immutable(hc=true)} on a field says the container it holds cannot change. It says nothing about the
 * elements, which are its hidden content. So this value may only be applied where the whole field object is
 * reached, never on a content tier ({@code §es}, {@code ∋}, {@code ∈}, or an indexed getter): otherwise a promise
 * about the container would silently be read as a promise about everything inside it. Each caller is responsible
 * for that check; this class only supplies the value.
 *
 * <h2>Contract only, for now</h2>
 * Nothing infers the property yet — only a hand-written annotation, via {@code SourceContractMaterializer}. On
 * unannotated code every method here returns null and the declared type decides exactly as before, which is why
 * this whole feature is inert on the corpus. Inferring it is inter-procedural; see
 * {@code docs/dynamic-immutability-feasibility.md}.
 */
final class DynamicImmutability {

    private DynamicImmutability() {
        // utility
    }

    /**
     * The field's contracted dynamic immutability, or {@code null} when there is none or it is too weak to say
     * anything about sharing. Below immutable-HC is ignored rather than returned: a contracted
     * {@code @FinalFields} must not be read as an independence claim.
     */
    static Value.Immutable ofField(FieldInfo fieldInfo) {
        Value.Immutable dynamic = fieldInfo.analysis().getOrNull(PropertyImpl.IMMUTABLE_FIELD,
                ValueImpl.ImmutableImpl.class);
        return dynamic != null && dynamic.isAtLeastImmutableHC() ? dynamic : null;
    }

    /**
     * {@code fromDeclaredType} improved by the field's dynamic immutability. Combined with {@code max}, never
     * replaced: dynamic immutability is extra evidence and may only improve a verdict, so a contract can never
     * lower a field whose declared type is already immutable.
     * <p>
     * A null {@code fromDeclaredType} means "undecided, wait". A definite contract removes the reason to wait,
     * because the declared type's own immutability can no longer change the answer downwards.
     */
    static Value.Independent improve(Value.Independent fromDeclaredType, FieldInfo fieldInfo) {
        Value.Immutable dynamic = ofField(fieldInfo);
        if (dynamic == null) return fromDeclaredType;
        Value.Independent fromDynamic = dynamic.toCorrespondingIndependent();
        return fromDeclaredType == null ? fromDynamic : fromDeclaredType.max(fromDynamic);
    }
}
