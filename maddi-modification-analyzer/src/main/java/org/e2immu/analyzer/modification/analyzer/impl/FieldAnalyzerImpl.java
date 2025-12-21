package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.analyzer.FieldAnalyzer;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.INDEPENDENT;

public class FieldAnalyzerImpl extends CommonAnalyzerImpl implements FieldAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldAnalyzerImpl.class);

    private final Runtime runtime;
    private final AnalysisHelper analysisHelper = new AnalysisHelper();

    public FieldAnalyzerImpl(Runtime runtime, IteratingAnalyzer.Configuration configuration) {
        super(configuration);
        this.runtime = runtime;
    }

    private record OutputImpl(List<AnalyzerException> analyzerExceptions, Set<Info> waitFor) implements Output {
    }


    @Override
    public Output go(FieldInfo fieldInfo, boolean cycleBreakingActive) {
        List<AnalyzerException> analyzerExceptions = new LinkedList<>();
        InternalFieldAnalyzer analyzer = new InternalFieldAnalyzer();
        try {
            analyzer.go(fieldInfo, cycleBreakingActive);
        } catch (RuntimeException re) {
            if (configuration.storeErrors()) {
                if (!(re instanceof AnalyzerException)) {
                    analyzerExceptions.add(new AnalyzerException(fieldInfo, re));
                }
            } else throw re;
        }
        return new OutputImpl(analyzerExceptions, analyzer.waitFor);
    }

    private class InternalFieldAnalyzer {
        private final Set<Info> waitFor = new HashSet<>();

        private void go(FieldInfo fieldInfo, boolean cycleBreakingActive) {
            LOGGER.debug("Do field {}", fieldInfo);
            Links linkedVariablesDone = fieldInfo.analysis().getOrNull(LinksImpl.LINKS, LinksImpl.class);
            Value.Bool unmodifiedDone = fieldInfo.analysis().getOrNull(PropertyImpl.UNMODIFIED_FIELD,
                    ValueImpl.BoolImpl.class);
            Value.Independent independentDone = fieldInfo.analysis().getOrNull(PropertyImpl.INDEPENDENT_FIELD,
                    Value.Independent.class);

            List<MethodInfo> methodsReferringToField = fieldInfo.owner().primaryType()
                    .recursiveSubTypeStream()
                    .flatMap(TypeInfo::constructorAndMethodStream)
                    .filter(mi -> notEmptyOrSyntheticAccessorAndReferringTo(mi, fieldInfo))
                    .toList();

            if (unmodifiedDone == null || unmodifiedDone.isFalse()) {
                Value.Bool unmodified = computeUnmodified(fieldInfo, methodsReferringToField);
                if (unmodified != null) {
                    if (fieldInfo.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_FIELD, unmodified)) {
                        DECIDE.debug("FI: Decide unmodified of field {} = {}", fieldInfo, unmodified);
                    }
                } else if (cycleBreakingActive) {
                    boolean write = fieldInfo.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_FIELD, TRUE);
                    assert write;
                    DECIDE.info("FI: Decide unmodified of field {} = true by {}", fieldInfo, highlight("cycleBreaking"));
                } else {
                    UNDECIDED.debug("FI: Unmodified of field {} undecided, wait for {}", fieldInfo, waitFor);
                }
            }

            Links linkedVariables = linkedVariablesDone != null ? linkedVariablesDone
                    : computeLinkedVariables(fieldInfo, methodsReferringToField);
            if (linkedVariables == null) {
                if (cycleBreakingActive) {
                    boolean write = fieldInfo.analysis().setAllowControlledOverwrite(LinksImpl.LINKS, LinksImpl.EMPTY);
                    assert write;
                    DECIDE.info("FI: Decide linked variables of field {} = empty by {}", fieldInfo, CYCLE_BREAKING);
                } else {
                    UNDECIDED.debug("FI: Linked variables of field {} undecided, wait for {}", fieldInfo, waitFor);
                    return;
                }
            }
            if (fieldInfo.analysis().setAllowControlledOverwrite(LinksImpl.LINKS, linkedVariables)) {
                DECIDE.debug("FI: Decide linked variables of field {} = {}", fieldInfo, linkedVariables);
            }
            if (independentDone == null) {
                Value.Independent independent = computeIndependent(fieldInfo, linkedVariables);
                if (independent != null) {
                    if (fieldInfo.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_FIELD, independent)) {
                        DECIDE.debug("FI: Decide independent of field {} = {}", fieldInfo, independent);
                    }
                } else if (cycleBreakingActive) {
                    boolean write = fieldInfo.analysis().setAllowControlledOverwrite(PropertyImpl.INDEPENDENT_FIELD, INDEPENDENT);
                    assert write;
                    DECIDE.info("FI: Decide independent of field {} = INDEPENDENT by {}", fieldInfo, CYCLE_BREAKING);
                } else {
                    UNDECIDED.debug("FI: Independent of field {} undecided, wait for {}", fieldInfo, waitFor);
                }
            }
        }

        private boolean notEmptyOrSyntheticAccessorAndReferringTo(MethodInfo mi, FieldInfo fieldInfo) {
            if (!mi.methodBody().isEmpty()) {
                VariableData vd = VariableDataImpl.of(mi.methodBody().lastStatement());
                return vd.variableInfoStream().anyMatch(vi ->
                        vi.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo);
            }
            Value.FieldValue fieldValue = mi.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                    ValueImpl.GetSetValueImpl.EMPTY);
            return fieldValue.field() == fieldInfo;
        }

        private Links computeLinkedVariables(FieldInfo fieldInfo, List<MethodInfo> methodsReferringToField) {
            Links.Builder builder = new LinksImpl.Builder(runtime.newFieldReference(fieldInfo));
            boolean undecided = false;
            for (MethodInfo methodInfo : methodsReferringToField) {
                Value.FieldValue fieldValue = methodInfo.analysis().getOrDefault(PropertyImpl.GET_SET_FIELD,
                        ValueImpl.GetSetValueImpl.EMPTY);
                if (fieldInfo == fieldValue.field()) {
                    assert !methodInfo.isConstructor();
                    //builder.add(runtime.newFieldReference(fieldInfo), LVImpl.LINK_DEPENDENT);
                    // FIXME not linking to myself?
                } else {
                    assert !methodInfo.methodBody().isEmpty();
                    VariableData vd = VariableDataImpl.of(methodInfo.methodBody().lastStatement());
                    for (VariableInfo vi : vd.variableInfoIterable()) {
                        if (vi.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo) {
                            Links lv = vi.linkedVariables();
                            if (lv == null) {
                                // no linked variables yet
                                waitFor.add(methodInfo);
                                undecided = true;
                            } else if (!lv.isEmpty()) {
                                // we're only interested in parameters, other fields, return values
                                for (Link l : lv) {
                                    if (!(l.to() instanceof LocalVariable)) {
                                        builder.add(l.linkNature(), l.to());
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
                    LOGGER.debug("Getters/setters are never modifying");
                } else if (!methodInfo.isConstructor() && !poc.infoSet().contains(methodInfo)) {
                    Statement lastStatement = methodInfo.methodBody().lastStatement();
                    assert lastStatement != null;
                    VariableData vd = VariableDataImpl.of(lastStatement);
                    for (VariableInfo vi : vd.variableInfoIterable()) {
                        if (vi.variable() instanceof FieldReference fr && fr.fieldInfo() == fieldInfo) {
                            Value.Bool v = vi.analysis().getOrNull(VariableInfoImpl.UNMODIFIED_VARIABLE,
                                    ValueImpl.BoolImpl.class);
                            if (v == null) {
                                waitFor.add(methodInfo);
                                undecided = true;
                            } else if (v.isFalse()) {
                                return FALSE;
                            }
                        }
                    }
                }
            }
            return undecided ? null : TRUE;
        }

        private Value.Independent computeIndependent(FieldInfo fieldInfo, Links links) {
            Value.Independent independentOfType = analysisHelper.typeIndependentFromImmutableOrNull(fieldInfo.owner(),
                    fieldInfo.type());
            if (independentOfType == null) {
                TypeInfo bestType = fieldInfo.type().bestTypeInfo();
                assert bestType != null;
                waitFor.add(bestType);
                return null;
            }
            if (independentOfType.isIndependent()) return INDEPENDENT;
            Value.Independent independent = INDEPENDENT;
            for (Link link : links) {
                if (link.to() instanceof ParameterInfo pi && !pi.methodInfo().access().isPrivate()
                    || link.to() instanceof ReturnVariable rv && !rv.methodInfo().access().isPrivate()) {
                    Value.Independent toIndependent = analysisHelper.typeIndependent(link.to().parameterizedType());
                    independent = independent.min(toIndependent);
                }
            }
            return independentOfType.max(independent);
        }
    }
}
