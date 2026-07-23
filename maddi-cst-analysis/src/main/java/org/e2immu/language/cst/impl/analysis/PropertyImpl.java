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

package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Property.AnalysisTier;
import org.e2immu.language.cst.api.analysis.Value;

public class PropertyImpl implements Property {
    // type
    public static final Property IMMUTABLE_TYPE = new PropertyImpl("immutableType",
            ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_TYPE = new PropertyImpl("containerType");
    public static final Property INDEPENDENT_TYPE = new PropertyImpl("independentType",
            ValueImpl.IndependentImpl.DEPENDENT);
    public static final Property IMMUTABLE_TYPE_INDEPENDENT_OF_TYPE_PARAMETERS
            = new PropertyImpl("immutableTypeDeterminedByParameters");
    public static final Property FINAL_TYPE = new PropertyImpl("finalType");
    public static final Property UTILITY_CLASS = new PropertyImpl("utilityClass");
    /**
     * Eventual immutability (road to immutability §060): the {@code after="…"} of {@code @Immutable},
     * {@code @ImmutableContainer}, {@code @FinalFields}. Deliberately kept out of {@link #IMMUTABLE_TYPE}, which
     * is combined with min/max all over the analyzer; an eventual value in that lattice would silently move
     * independence and guard decisions. This property records the promise, the lattice keeps recording what holds
     * unconditionally.
     */
    public static final Property EVENTUALLY_IMMUTABLE_TYPE = new PropertyImpl("eventuallyImmutableType",
            ValueImpl.EventuallyImmutableImpl.NOT_EVENTUAL);

    // method
    public static final Property NON_MODIFYING_METHOD = new PropertyImpl("nonModifyingMethod");
    /** the method has a STATIC SIDE EFFECT: it modifies static/global state belonging to a type other than its
     * own primary type (a modifying call on, or assignment to, another type's static field). Informational — it
     * does not by itself cap the type's immutability (immutability inspects only the type's own fields) — but it
     * is the "global-escape" arm of the confinement guard: a modification reached through an {@code
     * @IgnoreModifications} field that is a static side effect leaves the ignored stratum. See
     * road-to-immutability section 050 ("Static side effects") and section 050 "confinement guard". */
    public static final Property STATIC_SIDE_EFFECTS_METHOD = new PropertyImpl("staticSideEffectsMethod");
    /** the source-level analysis of this method was abandoned (cycle protection, work ceiling, fault
     * isolation) and its values come from the SHALLOW summary. Consumers that rely on per-call data
     * (e.g. VARIABLES_LINKED_TO_OBJECT for extract-interface) must treat such methods pessimistically. */
    public static final Property DEGRADED_ANALYSIS_METHOD = new PropertyImpl("degradedAnalysisMethod");
    public static final Property FLUENT_METHOD = new PropertyImpl("fluentMethod");
    public static final Property IDENTITY_METHOD = new PropertyImpl("identityMethod");
    public static final Property NOT_NULL_METHOD = new PropertyImpl("notNullMethod", ValueImpl.NotNullImpl.NULLABLE);
    public static final Property IGNORE_MODIFICATION_METHOD = new PropertyImpl("ignoreModMethod");
    public static final Property POST_CONDITIONS_METHOD = new PropertyImpl("postConditionsMethod",
            ValueImpl.PostConditionsImpl.EMPTY);
    public static final Property PRECONDITION_METHOD = new PropertyImpl("preconditionMethod",
            ValueImpl.PreconditionImpl.EMPTY);
    public static final Property INDICES_OF_ESCAPE_METHOD = new PropertyImpl("indicesOfEscapesNotInPrePostCondition",
            ValueImpl.IndicesOfEscapesImpl.EMPTY);
    public static final Property METHOD_ALLOWS_INTERRUPTS = new PropertyImpl("methodAllowsInterrupts");
    public static final Property OWN_FIELDS_READ_MODIFIED_IN_METHOD = new PropertyImpl("areOwnFieldsReadModified",
            ValueImpl.FieldBooleanMapImpl.EMPTY);
    public static final Property INDEPENDENT_METHOD = new PropertyImpl("independentMethod",
            ValueImpl.IndependentImpl.DEPENDENT);
    public static final Property FINALIZER_METHOD = new PropertyImpl("finalizerMethod");
    /** {@code @Mark}, {@code @Only}, {@code @TestMark} on a method: see {@link #EVENTUALLY_IMMUTABLE_TYPE}. */
    public static final Property EVENTUAL_METHOD = new PropertyImpl("eventualMethod",
            ValueImpl.EventualImpl.NOT_EVENTUAL);
    /**
     * The mark label(s) of {@code @NotModified(after="…")} on a method: the method modifies before the mark
     * (typically a lazy-loading getter that effects the transition) and is non-modifying after it. The
     * method-level twin of {@link #EVENTUALLY_FINAL_FIELD}. {@link #NON_MODIFYING_METHOD} keeps recording the
     * unconditional verdict, which for such a method is {@code false} (it does modify, before the mark).
     */
    public static final Property EVENTUALLY_NON_MODIFYING_METHOD = new PropertyImpl("eventuallyNonModifyingMethod",
            ValueImpl.SetOfStringsImpl.EMPTY_SET);
    // dynamic return type
    public static final Property IMMUTABLE_METHOD = new PropertyImpl("immutableMethod"
            , ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_METHOD = new PropertyImpl("containerMethod");

    // commutation on methods
    public static final Property PARALLEL_PARAMETER_GROUPS = new PropertyImpl("parallelParameterGroups",
            ValueImpl.ParameterParSeqImpl.EMPTY);
    public static final Property COMMUTABLE_METHODS = new PropertyImpl("commutableMethods",
            ValueImpl.CommutableDataImpl.NONE);
    // carryOnRewire: GET_SET_FIELD is parse-time (record synthetics, KotlinScan via FactoryImpl.setGetSetField).
    // A REWIRE'd type is never re-parsed, so if not carried it is lost and prep does not re-derive it; carry it,
    // GetSetValueImpl.rewire re-points the field through the infoMap. See docs/analysis-rewiring.md / docs/rewiring.md.
    public static final Property GET_SET_FIELD = new PropertyImpl("getSetField",
            ValueImpl.GetSetValueImpl.EMPTY, true);
    public static final Property GET_SET_EQUIVALENT = new PropertyImpl("getSetEquivalent",
            ValueImpl.GetSetEquivalentImpl.EMPTY);
    public static final Property IMPLEMENTATIONS = new PropertyImpl("implementations",
            ValueImpl.SetOfMethodInfoImpl.EMPTY);

    // parameter
    public static final Property UNMODIFIED_PARAMETER = new PropertyImpl("unmodifiedParameter");
    /**
     * The mark label(s) of {@code @NotModified(after="…")} on a parameter: the method modifies the argument's
     * object graph only through chains the eventual machinery excuses -- once every label (field names in the
     * parameter type's label space) has been committed on the argument, a call leaves the argument unmodified.
     * The parameter twin of {@link #EVENTUALLY_NON_MODIFYING_METHOD}. {@link #UNMODIFIED_PARAMETER} keeps
     * recording the unconditional verdict, which for such a parameter is {@code false} (it is modified, before
     * the marks).
     */
    public static final Property EVENTUALLY_UNMODIFIED_PARAMETER = new PropertyImpl("eventuallyUnmodifiedParameter",
            ValueImpl.SetOfStringsImpl.EMPTY_SET);
    public static final Property IGNORE_MODIFICATIONS_PARAMETER = new PropertyImpl("ignoreModsParameter");
    public static final Property PARAMETER_ASSIGNED_TO_FIELD = new PropertyImpl("parameterAssignedToField",
            ValueImpl.AssignedToFieldImpl.EMPTY);
    public static final Property NOT_NULL_PARAMETER = new PropertyImpl("notNullParameter", ValueImpl.NotNullImpl.NULLABLE);
    public static final Property IMMUTABLE_PARAMETER = new PropertyImpl("immutableParameter"
            , ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_PARAMETER = new PropertyImpl("containerParameter");
    /** {@code @Mark} travelling to a parameter, when a marked method is called on it (road to immutability §060). */
    public static final Property EVENTUAL_PARAMETER = new PropertyImpl("eventualParameter",
            ValueImpl.EventualImpl.NOT_EVENTUAL);
    public static final Property INDEPENDENT_PARAMETER = new PropertyImpl("independentParameter",
            ValueImpl.IndependentImpl.DEPENDENT);
    public static final Property DOWNCAST_PARAMETER = new PropertyImpl("downcastParameter",
            ValueImpl.VariableToTypeInfoSetImpl.EMPTY);

    // field
    // INTRINSIC: prepwork's ComputePartOfConstructionFinalField re-derives this from the type's own body every run.
    public static final Property FINAL_FIELD = new PropertyImpl("finalField", ValueImpl.BoolImpl.FALSE,
            AnalysisTier.INTRINSIC);
    public static final Property NOT_NULL_FIELD = new PropertyImpl("notNullField", ValueImpl.NotNullImpl.NULLABLE);
    /**
     * The mark label(s) of {@code @Final(after="…")} / {@code @NotModified(after="…")}: the field is not final
     * (resp. is modified) before the mark, and is final (resp. unmodified) after it. {@link #FINAL_FIELD} keeps
     * recording plain, unconditional finality.
     */
    public static final Property EVENTUALLY_FINAL_FIELD = new PropertyImpl("eventuallyFinalField",
            ValueImpl.SetOfStringsImpl.EMPTY_SET);
    public static final Property IGNORE_MODIFICATIONS_FIELD = new PropertyImpl("ignoreModificationsField");
    public static final Property UNMODIFIED_FIELD = new PropertyImpl("unmodifiedField");
    public static final Property IMMUTABLE_FIELD = new PropertyImpl("immutableField"
            , ValueImpl.ImmutableImpl.MUTABLE);
    public static final Property CONTAINER_FIELD = new PropertyImpl("containerField");
    public static final Property INDEPENDENT_FIELD = new PropertyImpl("independentField",
            ValueImpl.IndependentImpl.DEPENDENT);
    //public static final Property DOWNCAST_FIELD = new PropertyImpl("downcastField", ValueImpl.SetOfTypeInfoImpl.EMPTY);

    // statement
    // INTRINSIC: prepwork's ComputeAlwaysEscapes re-derives this per statement every run.
    public static final Property ALWAYS_ESCAPES = new PropertyImpl("statementAlwaysEscapes",
            ValueImpl.BoolImpl.FALSE, AnalysisTier.INTRINSIC);

    // any element
    public static final Property DEFAULTS_ANALYZER = new PropertyImpl("defaultsAnalyzer");
    public static final Property ANNOTATED_API = new PropertyImpl("annotatedApi");
    public static final Property ANALYZER_ERROR = new PropertyImpl("analyzerError", ValueImpl.MessageImpl.EMPTY);

    // type parameter
    public static final Property INDEPENDENT_TYPE_PARAMETER = new PropertyImpl("independentTypeParameter",
            ValueImpl.IndependentImpl.DEPENDENT);

    // instanceof
    // INTRINSIC: prepwork's MethodAnalyzer re-derives the instanceof-pattern scope from the method body every run.
    public static final Property INSTANCEOF_SCOPE = new PropertyImpl("instanceOfScope", ValueImpl.ScopeImpl.EMPTY,
            AnalysisTier.INTRINSIC);

    private final String key;
    private final Value defaultValue;
    private final boolean carryOnRewire;
    private final AnalysisTier analysisTier;

    public PropertyImpl(String key) {
        this(key, ValueImpl.BoolImpl.FALSE);
    }

    public PropertyImpl(String key, Value defaultValue) {
        this(key, defaultValue, false);
    }

    public PropertyImpl(String key, Value defaultValue, boolean carryOnRewire) {
        this(key, defaultValue, carryOnRewire,
                carryOnRewire ? AnalysisTier.PARSE_TIME : AnalysisTier.CROSS_TYPE_DERIVED);
    }

    /** Explicit tier — used by the intrinsic (prepwork-recomputed) properties. */
    public PropertyImpl(String key, Value defaultValue, AnalysisTier analysisTier) {
        this(key, defaultValue, false, analysisTier);
    }

    private PropertyImpl(String key, Value defaultValue, boolean carryOnRewire, AnalysisTier analysisTier) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.carryOnRewire = carryOnRewire;
        this.analysisTier = analysisTier;
    }

    @Override
    public boolean carryOnRewire() {
        return carryOnRewire;
    }

    @Override
    public AnalysisTier analysisTier() {
        return analysisTier;
    }

    @Override
    public Value defaultValue() {
        return defaultValue;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Class<? extends Value> classOfValue() {
        return defaultValue.getClass();
    }

    @Override
    public String toString() {
        return key;
    }
}
