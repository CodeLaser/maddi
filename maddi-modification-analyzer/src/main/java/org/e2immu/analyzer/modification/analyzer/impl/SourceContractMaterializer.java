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

import org.e2immu.analyzer.modification.common.defaults.ContractReader;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IGNORE_MODIFICATIONS_FIELD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_FIELD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.IMMUTABLE_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.STATIC_SIDE_EFFECTS_METHOD;

/**
 * Writes a user's <em>dynamic type</em> contract on a SOURCE field or method into {@code analysis()}.
 * <p>
 * {@code AnnotationToProperty} turns annotations into property values, but it is only reached through
 * {@code ShallowTypeAnalyzer} / {@code ShallowMethodAnalyzer}, which handle elements the analyzer does not
 * see the source of. For a source element nothing puts the contract anywhere, so an {@code @Immutable} written
 * on a field or an accessor is read back by the {@link ContractReader} and by nothing else. Everything
 * downstream — the codec, the guard, the IDE daemon, and any future consumer of dynamic immutability — looks
 * in {@code analysis()}. This is the same gap the eventual analyzer closed for {@code EVENTUAL_METHOD}, and
 * it is closed the same way.
 *
 * <h2>Why only these two properties</h2>
 * Materializing a contract means the analyzer <b>trusts</b> the user rather than computing. That is defensible
 * exactly when the analyzer has no way of deriving the value for itself:
 * <ul>
 *   <li>{@link org.e2immu.language.cst.impl.analysis.PropertyImpl#IMMUTABLE_FIELD} and
 *       {@link org.e2immu.language.cst.impl.analysis.PropertyImpl#IMMUTABLE_METHOD} are <em>dynamic</em> type
 *       information: the declared type is {@code List<X>} while the object actually held or returned is
 *       immutable. Nothing in the declared type says so, and no source-level inference computes it today, so
 *       the contract is the only possible source. (Inferring it is a separate, inter-procedural problem — see
 *       {@code docs/dynamic-immutability-feasibility.md}.)</li>
 *   <li>Everything the analyzer <em>does</em> compute — {@code NON_MODIFYING_METHOD}, the {@code INDEPENDENT_*}
 *       and {@code CONTAINER_*} family, {@code FINAL_FIELD}, … — is deliberately NOT materialized. Trusting a
 *       wrong contract there would silently replace a derived verdict with an assertion, and comparing the two
 *       is precisely what guard mode exists for.</li>
 * </ul>
 * The guard reads neither of these two properties (it polices {@code NON_MODIFYING_METHOD} and
 * {@code INDEPENDENT_METHOD} on abstract methods, and {@code IMMUTABLE_TYPE}/{@code CONTAINER_TYPE} on types),
 * so materializing them cannot blunt any contract check that exists.
 *
 * <h2>Idempotent, and re-run every pass on purpose</h2>
 * A computed value always wins: we write only when nothing has been decided yet. And the write is repeated
 * each pass rather than done once on the first iteration, because {@code IteratingAnalyzerImpl}'s
 * clear-before-recompute ({@code clearDerivedFamily}) removes both properties along with the rest of the
 * derived family; a first-iteration-only materialization would be silently dropped on that path.
 */
public class SourceContractMaterializer {

    private final ContractReader contractReader;
    private final AtomicInteger propertyChanges;

    public SourceContractMaterializer(Runtime runtime, AtomicInteger propertyChanges) {
        this.contractReader = new ContractReader(runtime);
        this.propertyChanges = propertyChanges;
    }

    public void materialize(MethodInfo methodInfo) {
        materialize(methodInfo, IMMUTABLE_METHOD);
        // @StaticSideEffects is the global-escape twin of @IgnoreModifications: a pure contract on the safe
        // surface (e.g. System.setOut in an AAPI declaration) whose global effect the analyzer cannot see. On a
        // SOURCE method it is normally computed, but a source author may also assert it directly; materialize it
        // so it is read from analysis() like every other consumer expects. It caps nothing (SSE is informational,
        // feeding only the @IgnoreModifications containment guard), so trusting it here is safe.
        materializeTrueBool(methodInfo, STATIC_SIDE_EFFECTS_METHOD);
    }

    public void materialize(FieldInfo fieldInfo) {
        materialize(fieldInfo, IMMUTABLE_FIELD);
        // @IgnoreModifications is a pure contract -- it cannot be computed (it declares that the author does not
        // care about this field's modifications), so on a SOURCE field the annotation would otherwise be read by
        // nothing. Materializing it lets @IgnoreModifications-as-hidden-content (road-to-immutability section 050)
        // work on source exactly as it does on shallow/AAPI types. It cannot blunt any derived verdict, since
        // nothing computes IGNORE_MODIFICATIONS_FIELD.
        materializeTrueBool(fieldInfo, IGNORE_MODIFICATIONS_FIELD);
    }

    private void materialize(Info info, Property property) {
        if (info.analysis().haveAnalyzedValueFor(property)) return; // computed, or already materialized
        // the reader re-derives from the CST on every call; most elements carry no annotation at all, and of
        // those that do, @Override is by far the most common. Skip before paying for the walk.
        if (info.annotations().isEmpty()) return;
        if (contractReader.contracts(info).get(property) instanceof Value.Immutable immutable
            && !immutable.isDefault()) {
            // MUTABLE is the default: writing it would record an "annotation" indistinguishable from silence
            info.analysis().set(property, immutable);
            CommonAnalyzerImpl.DECIDE.debug("SCM: Contracted {} of {} = {}", property, info, immutable);
            propertyChanges.incrementAndGet();
        }
    }

    // as materialize(Info, Property) but for a boolean contract; only TRUE is written (FALSE is the silent
    // default, indistinguishable from an absent annotation)
    private void materializeTrueBool(Info info, Property property) {
        if (info.analysis().haveAnalyzedValueFor(property)) return;
        if (info.annotations().isEmpty()) return;
        if (contractReader.contracts(info).get(property) instanceof Value.Bool bool && bool.isTrue()) {
            info.analysis().set(property, bool);
            CommonAnalyzerImpl.DECIDE.debug("SCM: Contracted {} of {} = {}", property, info, bool);
            propertyChanges.incrementAndGet();
        }
    }
}
