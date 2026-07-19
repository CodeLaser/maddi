package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.common.util.TolerantWrite;
import org.e2immu.analyzer.modification.analyzer.FieldAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.analysis.Message;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;

public class FieldAnalyzerImpl extends CommonAnalyzerImpl implements FieldAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldAnalyzerImpl.class);

    private final Runtime runtime;
    private final AnalysisHelper analysisHelper = new AnalysisHelper();

    public FieldAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration,
                             AtomicInteger propertyChanges, List<Message> analyzerMessages) {
        super(configuration, propertyChanges, analyzerMessages);
        this.runtime = runtime;
    }

    @Override
    public void go(FieldInfo fieldInfo, boolean cycleBreakingActive) {
        InternalFieldAnalyzer analyzer = new InternalFieldAnalyzer();
        analyzer.go(fieldInfo, cycleBreakingActive);
    }

    private class InternalFieldAnalyzer {
        private void go(FieldInfo fieldInfo, boolean cycleBreakingActive) {
            LOGGER.debug("Do field {}", fieldInfo);
            Value.Bool unmodifiedDone = fieldInfo.analysis().getOrDefault(PropertyImpl.UNMODIFIED_FIELD, FALSE);
            Value.Independent currentIndependent = fieldInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_FIELD,
                    ValueImpl.IndependentImpl.DEPENDENT);

            List<MethodInfo> methodsReferringToField = fieldInfo.owner().primaryType()
                    .recursiveSubTypeStream()
                    .flatMap(TypeInfo::constructorAndMethodStream)
                    .filter(mi -> notEmptyOrSyntheticAccessorAndReferringTo(mi, fieldInfo))
                    .toList();

            if (unmodifiedDone.isFalse()) {
                Value.Bool unmodified = computeUnmodified(fieldInfo, methodsReferringToField);
                if (unmodified != null) {
                    if (TolerantWrite.setAllowControlledOverwrite(fieldInfo.analysis(), PropertyImpl.UNMODIFIED_FIELD, unmodified, fieldInfo)) {
                        DECIDE.debug("FI: Decide unmodified of field {} = {}", fieldInfo, unmodified);
                        propertyChanges.incrementAndGet();
                    }
                } else if (cycleBreakingActive) {
                    // not asserted: under the MODREACH freeze (P2.3b re-derivation) this write is
                    // refused by design — the reachability pass is the single writer
                    boolean write = TolerantWrite.setAllowControlledOverwrite(fieldInfo.analysis(), PropertyImpl.UNMODIFIED_FIELD, TRUE, fieldInfo);
                    if (write) {
                        DECIDE.info("FI: Decide unmodified of field {} = true by {}", fieldInfo, highlight("cycleBreaking"));
                        propertyChanges.incrementAndGet();
                    }
                } else {
                    UNDECIDED.debug("FI: Unmodified of field {} undecided", fieldInfo);
                }
            }

            Links linkedVariables = computeLinkedVariables(fieldInfo, methodsReferringToField);
            if (linkedVariables == null) {
                if (cycleBreakingActive) {
                    // idempotent: on re-runs of the breaking pass the value may already stand (write=false);
                    // the former 'assert write' + unconditional fall-through-with-null crashed 200+ elements
                    // when cycle breaking first activated at corpus scale
                    if (TolerantWrite.setAllowControlledOverwrite(fieldInfo.analysis(), LinksImpl.LINKS,
                            LinksImpl.EMPTY, fieldInfo)) {
                        DECIDE.info("FI: Decide linked variables of field {} = empty by {}", fieldInfo, CYCLE_BREAKING);
                        propertyChanges.incrementAndGet();
                    }
                    linkedVariables = fieldInfo.analysis().getOrDefault(LinksImpl.LINKS, LinksImpl.EMPTY);
                } else {
                    UNDECIDED.debug("FI: Linked variables of field {} undecided", fieldInfo);
                    return;
                }
            }
            if (TolerantWrite.setAllowControlledOverwrite(fieldInfo.analysis(), LinksImpl.LINKS, linkedVariables, fieldInfo)) {
                DECIDE.debug("FI: Decide linked variables of field {} = {}", fieldInfo, linkedVariables);
                propertyChanges.incrementAndGet();
            }
            if (!currentIndependent.isIndependent()) {
                Value.Independent independent = computeIndependent(fieldInfo, linkedVariables);
                if (independent != null) {
                    if (TolerantWrite.setAllowControlledOverwrite(fieldInfo.analysis(), PropertyImpl.INDEPENDENT_FIELD, independent, fieldInfo)) {
                        DECIDE.debug("FI: Decide independent of field {} = {}", fieldInfo, independent);
                        propertyChanges.incrementAndGet();
                    }
                } else if (cycleBreakingActive) {
                    boolean write = TolerantWrite.setAllowControlledOverwrite(fieldInfo.analysis(), PropertyImpl.INDEPENDENT_FIELD, INDEPENDENT, fieldInfo);
                    assert write;
                    DECIDE.info("FI: Decide independent of field {} = INDEPENDENT by {}", fieldInfo, CYCLE_BREAKING);
                    propertyChanges.incrementAndGet();
                } else {
                    UNDECIDED.debug("FI: Independent of field {} undecided", fieldInfo);
                }
            }
        }

        private boolean notEmptyOrSyntheticAccessorAndReferringTo(MethodInfo mi, FieldInfo fieldInfo) {
            if (!mi.methodBody().isEmpty()) {
                VariableData vd = VariableDataImpl.of(mi.methodBody().lastStatement());
                // null when the last statement carries no variable data, e.g. a constructor whose only
                // statement is the synthetic super() (openjdk keeps it; it has no variable data): no field refs
                if (vd == null) return false;
                return vd.variableInfoStream().anyMatch(vi ->
                        vi.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo);
            }
            Value.FieldValue fieldValue = mi.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                    ValueImpl.GetSetValueImpl.EMPTY);
            return fieldValue.field() == fieldInfo;
        }

        private Links computeLinkedVariables(FieldInfo fieldInfo, List<MethodInfo> methodsReferringToField) {
            FieldReference primary = runtime.newFieldReference(fieldInfo);
            Links.Builder builder = new LinksImpl.Builder(primary);
            boolean undecided = false;
            for (MethodInfo methodInfo : methodsReferringToField) {
                if (!methodInfo.methodBody().isEmpty()) {
                    VariableData vd = VariableDataImpl.of(methodInfo.methodBody().lastStatement());
                    for (VariableInfo vi : vd.variableInfoIterable()) {
                        if (vi.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo) {
                            Links lv = vi.linkedVariables();
                            if (lv == null) {
                                // no linked variables yet
                                undecided = true;
                            } else if (!lv.isEmpty()) {
                                // we're only interested in parameters, other fields, return values
                                for (Link l : lv) {
                                    if (LinkVariable.acceptForLinkedVariables(l.to())
                                        && primary.equals(Util.primary(l.from()))
                                        // avoid duplicate links
                                        && !builder.contains(l.from(), l.linkNature(), l.to())) {
                                        builder.add(l.from(), l.linkNature(), l.to());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return undecided ? null : builder.build();
        }


        private Value.Bool computeUnmodified(FieldInfo fieldInfo, List<MethodInfo> methodsReferringToField) {
            Value.SetOfInfo poc = fieldInfo.owner().analysis().getOrDefault(PART_OF_CONSTRUCTION,
                    EMPTY_PART_OF_CONSTRUCTION);
            boolean undecided = false;
            for (MethodInfo methodInfo : methodsReferringToField) {
                Value.FieldValue fieldValue = methodInfo.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                        ValueImpl.GetSetValueImpl.EMPTY);
                if (fieldInfo == fieldValue.field()) {
                    LOGGER.debug("Getters/setters cannot modify the field {}", fieldInfo);
                } else if (!methodInfo.isConstructor() && !poc.infoSet().contains(methodInfo)) {
                    Statement lastStatement = methodInfo.methodBody().lastStatement();
                    assert lastStatement != null;
                    VariableData vd = VariableDataImpl.of(lastStatement);
                    for (VariableInfo vi : vd.variableInfoIterable()) {
                        if (vi.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo) {
                            Value.Bool v = vi.analysis().getOrNull(VariableInfoImpl.UNMODIFIED_VARIABLE,
                                    ValueImpl.BoolImpl.class);
                            if (v == null) {
                                undecided = true;
                            } else if (v.isFalse()) {
                                return FALSE;
                            }
                        }
                    }
                }
            }
            Value.Bool viaInheritedDefault = modifiableThroughInheritedDefaultMethod(fieldInfo);
            if (viaInheritedDefault == null) undecided = true;
            else if (viaInheritedDefault.isFalse()) return FALSE;
            return undecided ? null : TRUE;
        }

        /*
        A modifying 'default' method inherited (not overridden) from an interface can reach this field through an
        abstract accessor that the owner overrides to return it: interface Buffer { List<String> items();
        default void add(String s) { items().add(s); } } — the default method is analyzed once, in the interface's
        context, where the accessor backs no field, so its summary records only 'modifies this'; the field-level
        detail is not recoverable there. Sound over-approximation on the owner's side: when the owner inherits a
        modifying default method it does not override, every field backed by an accessor overriding an interface
        accessor is modifiable through it. (notes/default-method-modification-not-propagated-to-impl-field.md)

        Returns FALSE = modifiable through such a method, TRUE = no such method, null = a candidate default
        method's modification status is not yet decided.
         */
        private Value.Bool modifiableThroughInheritedDefaultMethod(FieldInfo fieldInfo) {
            TypeInfo owner = fieldInfo.owner();
            boolean accessorOverridesInterface = owner.methodStream().anyMatch(accessor -> {
                Value.FieldValue afv = accessor.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                        ValueImpl.GetSetValueImpl.EMPTY);
                return afv.field() == fieldInfo && !afv.setter()
                       && accessor.overrides().stream().anyMatch(o -> o.isAbstract() && o.typeInfo().isInterface());
            });
            if (!accessorOverridesInterface) return TRUE;
            boolean undecided = false;
            for (TypeInfo superType : owner.superTypesExcludingJavaLangObject()) {
                if (!superType.isInterface()) continue;
                for (MethodInfo d : superType.methods()) {
                    if (d.isAbstract() || d.isStatic() || d.access().isPrivate()) continue;
                    boolean overriddenByOwner = owner.methodStream().anyMatch(m -> m.overrides().contains(d));
                    if (overriddenByOwner) continue; // the owner's own version is analyzed with the field visible
                    Value.Bool nonModifying = d.analysis().getOrNull(PropertyImpl.NON_MODIFYING_METHOD,
                            ValueImpl.BoolImpl.class);
                    if (nonModifying == null) undecided = true;
                    else if (nonModifying.isFalse()) return FALSE;
                }
            }
            return undecided ? null : TRUE;
        }

        private Value.Independent computeIndependent(FieldInfo fieldInfo, Links links) {
            TypeInfo owner = fieldInfo.owner();
            Value.Independent independentOfType = analysisHelper.typeIndependentFromImmutableOrNull(owner,
                    fieldInfo.type());
            if (independentOfType == null) {
                // wait
                return null;
            }
            if (independentOfType.isIndependent()) return INDEPENDENT;
            Value.Independent independent = INDEPENDENT;
            for (Link link : links) {
                if (link.to() instanceof ParameterInfo pi && !pi.methodInfo().access().isPrivate()
                    && owner.inHierarchyOf(pi.typeInfo())
                    || link.to() instanceof ReturnVariable rv && !rv.methodInfo().access().isPrivate()
                       && owner.inHierarchyOf(rv.methodInfo().typeInfo())) {
                    Value.Independent toIndependent;
                    if (link.from().equals(links.primary()) && link.linkNature().isIdenticalToOrAssignedFromTo()) {
                        // direct link
                        toIndependent = independentOfType;//already computed
                    } else if (!link.linkNature().isDecoration()) {
                        // a part of the field is linked to a parameter or return value. The
                        // TRANSPORTED CONTENT decides dependence, not the container: a content-tier
                        // link over IMMUTABLE elements (defensive copy `this.coords[i] = c[i]` of an
                        // int[]) copies values — nothing is shared, no aliasing, no dependence.
                        // Adjudicated in immutability-transform-divergence.md: this imprecision
                        // capped Point at @FinalFields while its loop-desugared twin (whose bridge
                        // drops the spurious link) correctly reached @Immutable(hc=true).
                        org.e2immu.language.cst.api.type.ParameterizedType transported =
                                link.from() instanceof org.e2immu.language.cst.api.variable.DependentVariable dv
                                        ? dv.parameterizedType()
                                        : fieldInfo.type().arrays() > 0
                                        ? fieldInfo.type().copyWithOneFewerArrays()
                                        : link.to().parameterizedType();
                        Value.Immutable transportedImm = analysisHelper.typeImmutable(transported);
                        if (transportedImm != null && transportedImm.isImmutable()) {
                            toIndependent = null; // immutable content transmits no dependence
                        } else {
                            toIndependent = analysisHelper.typeIndependentFromImmutableOrNull(link.to().parameterizedType());
                        }
                    } else {
                        toIndependent = null;
                    }
                    if (toIndependent != null) {
                        independent = independent.min(toIndependent);
                    }
                }
            }
            return independentOfType.max(independent);
        }
    }
}
