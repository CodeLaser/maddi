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

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.TypeModIndyAnalyzer;
import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.MethodLinkedVariables;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.*;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;

/*
Phase 3.

Does modification and independence of parameters, and independence, fluent, identity of methods.
Also breaks internal cycles.

Modification of methods and linking of variables is done in Phase 1.
Linking, modification and independence of fields is done in Phase 2.
Immutable, independence of types is done in Phase 4.1.

Strategy:
method independence, parameter independence can directly be read from linked variables computed in field analyzer.
parameter modification is computed as the combination of links to fields and local modifications.

 */
public class TypeModIndyAnalyzerImpl extends CommonAnalyzerImpl implements TypeModIndyAnalyzer {
    private final AnalysisHelper analysisHelper = new AnalysisHelper();

    public TypeModIndyAnalyzerImpl(IteratingAnalyzer.Configuration configuration) {
        super(configuration);
    }

    private record OutputImpl(List<AnalyzerException> analyzerExceptions, boolean resolvedInternalCycles,
                              Map<MethodInfo, Set<MethodInfo>> waitForMethodModifications,
                              Map<MethodInfo, Set<TypeInfo>> waitForTypeIndependence) implements Output {
    }

    @Override
    public Output go(TypeInfo typeInfo, Map<MethodInfo, Set<MethodInfo>> methodsWaitFor, boolean cycleBreakingActive) {
        InternalAnalyzer ia = new InternalAnalyzer(cycleBreakingActive);
        try {
            ia.go(typeInfo);
        } catch (RuntimeException re) {
            if (configuration.storeErrors()) {
                if (!(re instanceof AnalyzerException)) {
                    ia.analyzerExceptions.add(new AnalyzerException(typeInfo, re));
                }
            } else throw re;
        }
        return new OutputImpl(ia.analyzerExceptions, false, ia.waitForMethodModifications,
                ia.waitForTypeIndependence);
    }

    class InternalAnalyzer {
        List<AnalyzerException> analyzerExceptions = new LinkedList<>();
        Map<MethodInfo, Set<MethodInfo>> waitForMethodModifications = new HashMap<>();
        Map<MethodInfo, Set<TypeInfo>> waitForTypeIndependence = new HashMap<>();
        Map<MethodInfo, Set<FieldInfo>> waitForField = new HashMap<>();
        final boolean cycleBreakingActive;

        InternalAnalyzer(boolean cycleBreakingActive) {
            this.cycleBreakingActive = cycleBreakingActive;
        }

        private void go(TypeInfo typeInfo) {
            typeInfo.constructorAndMethodStream()
                    .filter(mi -> !mi.isAbstract())
                    .forEach(this::go);
            fromNonFinalFieldToParameter(typeInfo);
        }

        private void go(MethodInfo methodInfo) {
            FieldValue fieldValue = methodInfo.analysis().getOrDefault(GET_SET_FIELD, ValueImpl.GetSetValueImpl.EMPTY);
            if (fieldValue.field() != null) {
                handleGetterSetter(methodInfo, fieldValue);
            } else if (methodInfo.explicitlyEmptyMethod()) {
                handleExplicitlyEmptyMethod(methodInfo);
            } else if (!methodInfo.methodBody().isEmpty()) {
                handleNormalMethod(methodInfo);
            }
        }

        private void handleNormalMethod(MethodInfo methodInfo) {
            Statement lastStatement = methodInfo.methodBody().lastStatement();
            assert lastStatement != null;
            doIdentityAnalysis(methodInfo);
            doFluentAnalysis(methodInfo);
            MethodLinkedVariables mlv = methodInfo.analysis().getOrDefault(METHOD_LINKS, MethodLinkedVariablesImpl.EMPTY);
            doIndependent(methodInfo, mlv);

            for (ParameterInfo pi : methodInfo.parameters()) {
                handleParameter(methodInfo, pi, mlv);
            }
        }

        private void doFluentAnalysis(MethodInfo methodInfo) {
            MethodLinkedVariables mlv = methodInfo.analysis().getOrDefault(METHOD_LINKS,
                    MethodLinkedVariablesImpl.EMPTY);
            boolean identityFluent;
            if (methodInfo.hasReturnValue() && !mlv.ofReturnValue().isEmpty()) {
                identityFluent = mlv.ofReturnValue().stream()
                        .anyMatch(v -> v.to() instanceof This thisVar
                                       && (thisVar.typeInfo() == methodInfo.typeInfo()
                                           || thisVar.typeInfo().typeHierarchyExcludingJLOStream()
                                                   .anyMatch(h -> h.equals(methodInfo.typeInfo()))));
            } else {
                identityFluent = false;
            }
            if (methodInfo.analysis().setAllowControlledOverwrite(FLUENT_METHOD, ValueImpl.BoolImpl.from(identityFluent))) {
                DECIDE.debug("MI: Decide @Fluent of {} = {}", methodInfo, identityFluent);
            }
        }

        private void doIdentityAnalysis(MethodInfo methodInfo) {
            MethodLinkedVariables mlv = methodInfo.analysis().getOrDefault(METHOD_LINKS,
                    MethodLinkedVariablesImpl.EMPTY);
            boolean identity;
            if (methodInfo.hasReturnValue()
                && !methodInfo.parameters().isEmpty()
                && !mlv.ofReturnValue().isEmpty()) {
                Variable primaryFrom = mlv.ofReturnValue().primary();
                Variable p0 = methodInfo.parameters().getFirst();
                identity = mlv.ofReturnValue().stream().allMatch(link -> {
                    if (link.from().equals(primaryFrom)) return link.to().equals(p0);
                    ParameterInfo pi = Util.parameterPrimaryOrNull(link.to());
                    return p0.equals(pi);
                });
            } else {
                identity = false;
            }
            if (methodInfo.analysis().setAllowControlledOverwrite(IDENTITY_METHOD, ValueImpl.BoolImpl.from(identity))) {
                DECIDE.debug("MI: Decide @Identity of {} = {}", methodInfo, identity);
            }
        }

        private void handleParameter(MethodInfo methodInfo, ParameterInfo pi, MethodLinkedVariables mlv) {
            Value.Immutable imm = analysisHelper.typeImmutable(pi.parameterizedType());
            Bool unmodifiedInMethod;
            if (imm.isImmutable()) {
                unmodifiedInMethod = TRUE;
            } else {
                unmodifiedInMethod = ValueImpl.BoolImpl.from(!mlv.modified().contains(pi));
                Links links = mlv.ofParameters().get(pi.index());
                if (!links.isEmpty() && unmodifiedInMethod.isTrue()) {
                    // FIXME if mutable, check for §m; if HC, check for a HC link such as ~
                    for (Link link : links) {
                        if (link.to() instanceof FieldReference fr && fr.scopeIsRecursivelyThis()) {
                            Bool unmodifiedField = fr.fieldInfo().analysis().getOrNull(UNMODIFIED_FIELD,
                                    ValueImpl.BoolImpl.class);
                            if (unmodifiedField == null) {
                                unmodifiedInMethod = null;
                                waitForField.computeIfAbsent(methodInfo, m -> new HashSet<>()).add(fr.fieldInfo());
                                break;
                            } else if (unmodifiedField.isFalse()) {
                                unmodifiedInMethod = FALSE;
                                break;
                            }
                        }
                    }
                }
            }
            if (unmodifiedInMethod != null) {
                if (pi.analysis().setAllowControlledOverwrite(UNMODIFIED_PARAMETER, unmodifiedInMethod)) {
                    DECIDE.debug("MI: unmodified of parameter {} = {}", pi, unmodifiedInMethod);
                }
            } else if (cycleBreakingActive) {
                UNDECIDED.info("MI: Unmodified of parameter {} undecided, waitForField {}", pi, waitForField);
            } else {
                UNDECIDED.debug("MI: Unmodified of parameter {} undecided, waitForField {}", pi, waitForField);
            }
        }

        private void handleExplicitlyEmptyMethod(MethodInfo methodInfo) {
            if (methodInfo.analysis().setAllowControlledOverwrite(PropertyImpl.NON_MODIFYING_METHOD, TRUE)) {
                DECIDE.debug("MI: Decide non-modifying of method {} = true", methodInfo);
            }
            if (methodInfo.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_METHOD, INDEPENDENT)) {
                DECIDE.debug("MI: Decide independent of method {} = independent", methodInfo);
            }
            for (ParameterInfo pi : methodInfo.parameters()) {
                if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_PARAMETER, TRUE)) {
                    DECIDE.debug("MI: Decide unmodified of parameter {} = true", pi);
                }
                if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_PARAMETER, INDEPENDENT)) {
                    DECIDE.debug("MI: Decide independent of parameter {} = independent", pi);
                }
            }
        }

        private void handleGetterSetter(MethodInfo methodInfo, FieldValue fieldValue) {
            assert !methodInfo.isConstructor();
            // getter, setter
            Bool nonModifying = ValueImpl.BoolImpl.from(!fieldValue.setter());
            methodInfo.analysis().setAllowControlledOverwrite(NON_MODIFYING_METHOD, nonModifying);
            Independent independentFromType = analysisHelper.typeIndependentFromImmutableOrNull(fieldValue.field().type());
            if (independentFromType == null) {
                waitForTypeIndependence.computeIfAbsent(methodInfo, m -> new HashSet<>())
                        .add(fieldValue.field().type().bestTypeInfo());
                if (cycleBreakingActive) {
                    UNDECIDED.info("MI: Independent of method {} undecided, wait for type independence {}", methodInfo,
                            waitForTypeIndependence);
                } else {
                    UNDECIDED.debug("MI: Independent of method {} undecided, wait for type independence {}", methodInfo,
                            waitForTypeIndependence);
                }
            } else if (fieldValue.setter()) {
                handleSetter(methodInfo, fieldValue, independentFromType);
            } else {
                handleGetter(methodInfo, independentFromType);
            }
        }

        private void handleGetter(MethodInfo methodInfo, Independent independentFromType) {
            if (methodInfo.analysis().setAllowControlledOverwrite(INDEPENDENT_METHOD, independentFromType)) {
                DECIDE.debug("MI: Decide independent of method {} = {}}", methodInfo, independentFromType);
            }
            for (ParameterInfo pi : methodInfo.parameters()) {
                if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_PARAMETER, TRUE)) {
                    DECIDE.debug("MI: Decide unmodified of getter parameter: {} = true", pi);
                }
            }
        }

        private void handleSetter(MethodInfo methodInfo, FieldValue fieldValue, Independent independentFromType) {
            if (independentFromType.isIndependent()) {
                // must be unmodified, otherwise, we'll have to wait for a value to come from the field
                for (ParameterInfo pi : methodInfo.parameters()) {
                    if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_PARAMETER, TRUE)) {
                        DECIDE.debug("MI: Decide unmodified of parameter because independent: {} = true", pi);
                    }
                }
            } else {
                Bool unmodifiedField = fieldValue.field().analysis().getOrNull(UNMODIFIED_FIELD,
                        ValueImpl.BoolImpl.class);
                if (unmodifiedField == null) {
                    waitForField.computeIfAbsent(methodInfo, m -> new HashSet<>())
                            .add(fieldValue.field());
                    if (cycleBreakingActive) {
                        UNDECIDED.info("MI: Unmodified of setter parameter of {} undecided: wait for field {}",
                                methodInfo, waitForField);
                    } else {
                        UNDECIDED.debug("MI: Unmodified of setter parameter of {} undecided: wait for field {}",
                                methodInfo, waitForField);
                    }
                } else {
                    for (ParameterInfo pi : methodInfo.parameters()) {
                        if (pi.index() == fieldValue.parameterIndexOfIndex()) {
                            if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_PARAMETER, TRUE)) {
                                DECIDE.debug("MI: Decide unmodified of setter index parameter: {} = true", pi);
                            }
                        } else {
                            if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_PARAMETER, unmodifiedField)) {
                                DECIDE.debug("MI: Decide unmodified of setter parameter: {} = {}", pi, unmodifiedField);
                            }
                        }
                    }
                }
            }
        }

        /*
    constructors: independent
    void methods: independent
    fluent methods: because we return the same object that the caller already has, no more opportunity to make
        changes is leaked than what as already there. Independent!
    accessors: independent directly related to the immutability of the field being returned
    normal methods: does a modification to the return value imply any modification in the method's object?
        independent directly related to the immutability of the fields to which the return value links.
     */
        private void doIndependent(MethodInfo methodInfo, MethodLinkedVariables mlv) {

            Independent independentMethod = doIndependentMethod(methodInfo, mlv);
            if (independentMethod != null) {
                if (methodInfo.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_METHOD, independentMethod)) {
                    DECIDE.debug("MI: Decide independent of method {} = {}", methodInfo, independentMethod);
                }
            } else if (cycleBreakingActive) {
                boolean write = methodInfo.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_METHOD, INDEPENDENT);
                assert write;
                DECIDE.info("MI: Decide independent of method {} = INDEPENDENT by {}", methodInfo, CYCLE_BREAKING);
            } else {
                UNDECIDED.debug("MI: Independent of method undecided: {}", methodInfo);
            }
            for (ParameterInfo pi : methodInfo.parameters()) {
                Independent independent = doIndependentParameter(pi, mlv);
                if (independent != null) {
                    if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_PARAMETER, independent)) {
                        DECIDE.debug("MI: Decide independent of parameter {} = {}", pi, independent);
                    }
                } else if (cycleBreakingActive) {
                    boolean write = pi.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_PARAMETER, INDEPENDENT);
                    assert write;
                    DECIDE.info("MI: Decide independent of parameter {} = INDEPENDENT by {}", pi, CYCLE_BREAKING);
                } else {
                    UNDECIDED.debug("MI: Independent of parameter {} undecided", pi);
                }
            }
        }

        private Independent doIndependentParameter(ParameterInfo pi, MethodLinkedVariables mlv) {
            boolean typeIsImmutable = analysisHelper.typeImmutable(pi.parameterizedType()).isImmutable();
            if (typeIsImmutable) return INDEPENDENT;
            if (pi.methodInfo().isAbstract() || pi.methodInfo().methodBody().isEmpty()) return DEPENDENT;
            Links links = mlv.ofParameters().get(pi.index());
            return worstLinkToFields(links, pi.fullyQualifiedName());
        }

        private Independent doIndependentMethod(MethodInfo methodInfo, MethodLinkedVariables mlv) {
            if (methodInfo.isConstructor() || methodInfo.noReturnValue()) return INDEPENDENT;
            assert !methodInfo.isAbstract() : "Code only called when there is a method body";
            boolean fluent = methodInfo.analysis().getOrDefault(FLUENT_METHOD, FALSE).isTrue();
            if (fluent) return INDEPENDENT;
            boolean typeIsImmutable = analysisHelper.typeImmutable(methodInfo.returnType()).isImmutable();
            if (typeIsImmutable) return INDEPENDENT;
            return worstLinkToFields(mlv.ofReturnValue(), methodInfo.fullyQualifiedName());
        }

        private Independent worstLinkToFields(Links links, String variableFqn) {
            boolean immutableHc = false;
            for (Link link : links) {
                Variable primaryTo = Util.primary(link.to());
                if (primaryTo instanceof FieldReference fr && fr.scopeIsRecursivelyThis()) {
                    ParameterizedType type;
                    if (primaryTo == link.to() && link.linkNature().equals(LinkNatureImpl.IS_ASSIGNED_TO)) {
                        // this.set ← 0:set, TestFieldAnalyzer,1,2
                        type = primaryTo.parameterizedType();
                    } else if (link.linkNature().equals(LinkNatureImpl.SHARES_ELEMENTS)
                               || link.linkNature().equals(LinkNatureImpl.IS_SUPERSET_OF)) {
                        // 0:set.§cs⊇this.set.§cs, TestFieldAnalyzer,3
                        type = link.to().parameterizedType().copyWithoutArrays();
                    } else if (link.linkNature().equals(LinkNatureImpl.CONTAINS_AS_MEMBER)) {
                        // 0:element ∋ this.set.§cs
                        type = link.to().parameterizedType();
                    } else {
                        type = null;
                    }
                    if (type != null) {
                        Immutable fieldImmutable = analysisHelper.typeImmutable(type);
                        if (fieldImmutable.isMutable()) return DEPENDENT;
                        if (fieldImmutable.isImmutableHC()) immutableHc = true;
                    }
                }
            }
            return immutableHc ? INDEPENDENT_HC : INDEPENDENT;
        }


        private void fromNonFinalFieldToParameter(TypeInfo typeInfo) {
       /*   FIXME   Map<ParameterInfo, StaticValues> svMapParameters = collectReverseFromNonFinalFieldsToParameters(typeInfo);
            svMapParameters.forEach((pi, sv) -> {
                if (!pi.analysis().haveAnalyzedValueFor(STATIC_VALUES_PARAMETER)) {
                    pi.analysis().set(STATIC_VALUES_PARAMETER, sv);
                }
                if (sv.expression() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr && fr.scopeIsThis()) {
                    if (!pi.analysis().haveAnalyzedValueFor(PARAMETER_ASSIGNED_TO_FIELD)) {
                        pi.analysis().set(PARAMETER_ASSIGNED_TO_FIELD, new ValueImpl.AssignedToFieldImpl(Set.of(fr.fieldInfo())));
                    }
                }
            });
        }

        private Map<ParameterInfo, StaticValues> collectReverseFromNonFinalFieldsToParameters(TypeInfo typeInfo) {
            Map<ParameterInfo, StaticValues> svMapParameters = new HashMap<>();
            typeInfo.fields()
                    .stream()
                    .filter(f -> !f.isPropertyFinal())
                    .forEach(fieldInfo -> {
                        StaticValues sv = fieldInfo.analysis().getOrNull(STATIC_VALUES_FIELD, StaticValuesImpl.class);
                        if (sv != null
                            && sv.expression() instanceof VariableExpression ve
                            && ve.variable() instanceof ParameterInfo pi) {
                            VariableExpression reverseVe = runtime.newVariableExpressionBuilder()
                                    .setVariable(runtime.newFieldReference(fieldInfo))
                                    .setSource(pi.source())
                                    .build();
                            StaticValues reverse = StaticValuesImpl.of(reverseVe);
                            StaticValues prev = svMapParameters.put(pi, reverse);
                            if (prev != null && !prev.equals(sv)) throw new UnsupportedOperationException("TODO");
                        } else {
                            // FIXME waitFor
                        }
                    });
            return svMapParameters;*/
        }
    }
}
