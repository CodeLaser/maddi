package org.e2immu.analyzer.modification.analyzer.impl;

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.MethodModAnalyzer;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.common.defaults.ShallowMethodAnalyzer;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.LinkComputerImpl;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.*;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.FALSE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;

public class MethodModAnalyzerImpl extends CommonAnalyzerImpl implements MethodModAnalyzer, ModAnalyzerForTesting {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodModAnalyzerImpl.class);

    private final Runtime runtime;
    private final ShallowMethodAnalyzer shallowMethodAnalyzer;
    private final boolean trackObjectCreations;
    private final LinkComputer linkComputer;

    public MethodModAnalyzerImpl(JavaInspector javaInspector, IteratingAnalyzer.Configuration configuration) {
        super(configuration);
        this.runtime = javaInspector.runtime();
        shallowMethodAnalyzer = new ShallowMethodAnalyzer(runtime, Element::annotations);
        this.trackObjectCreations = configuration.trackObjectCreations();
        this.linkComputer = new LinkComputerImpl(javaInspector);
    }

    private record OutputImpl(List<AnalyzerException> analyzerExceptions, Set<MethodInfo> waitForMethods,
                              Set<TypeInfo> waitForIndependenceOfTypes,
                              Map<String, Integer> infoHistogram) implements Output {
    }

    @Override
    public Output go(MethodInfo methodInfo, boolean activateCycleBreaking) {
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(activateCycleBreaking);
        try {
            methodAnalyzer.doMethod(methodInfo);
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception/error analyzing {} @line {}", methodInfo, bestSourceLog(methodInfo), re);
            if (configuration.storeErrors()) {
                if (!(re instanceof AnalyzerException)) {
                    methodAnalyzer.analyzerExceptions.add(new AnalyzerException(methodInfo, re));
                }
            } else throw re;
        }
        return new OutputImpl(methodAnalyzer.analyzerExceptions, methodAnalyzer.waitForMethods,
                methodAnalyzer.waitForIndependenceOfTypes, methodAnalyzer.infoHistogram);
    }

    private static String bestSourceLog(MethodInfo methodInfo) {
        Block methodBody = methodInfo.methodBody();
        return methodBody == null || methodBody.source() == null ? "?" : methodBody.source().compact2();
    }

    class MethodAnalyzer {
        private final List<AnalyzerException> analyzerExceptions = new LinkedList<>();
        private final Set<MethodInfo> waitForMethods = new HashSet<>();
        private final Set<TypeInfo> waitForIndependenceOfTypes = new HashSet<>();
        private final Map<String, Integer> infoHistogram = new HashMap<>();
        private final boolean activateCycleBreaking;

        MethodAnalyzer(boolean activateCycleBreaking) {
            this.activateCycleBreaking = activateCycleBreaking;
        }

        public void doMethod(MethodInfo methodInfo) {
            LOGGER.debug("Mod: do method {}", methodInfo);

            if (methodInfo.isAbstract()) {
                // NOTE: the shallow analyzers only write out non-default values
                shallowMethodAnalyzer.analyze(methodInfo);
                // TODO consider moving this into the shallow analyzer!
            } else {
                methodInfo.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(methodInfo));
                Block methodBody = methodInfo.methodBody();
                if (LOGGER.isDebugEnabled() && methodBody.source() != null) {
                    LOGGER.debug("Mod:   method body @line {}", methodBody.source().compact2());
                }
                if (!methodBody.isEmpty()) {
                    VariableData variableData = VariableDataImpl.of(methodBody.statements().getLast());
                    copyFromVariablesIntoMethod(methodInfo, variableData);
                }
            }
        }

        private void copyFromVariablesIntoMethod(MethodInfo methodInfo, VariableData variableData) {
            boolean allFieldsUnmodified = true;
            for (VariableInfo vi : variableData.variableInfoStream().toList()) {
                Variable v = vi.variable();
                if (v instanceof ParameterInfo pi && pi.methodInfo() == methodInfo) {
                    copyFromVariablesIntoMethodPi(vi, pi);
                } else if (v instanceof This ||
                           (v instanceof FieldReference fr && (fr.scopeIsRecursivelyThis() || fr.isStatic()))
                           && fr.fieldInfo().analysis().getOrDefault(IGNORE_MODIFICATIONS_FIELD, FALSE).isFalse()
                           || vi.isVariableInClosure()) {
                    boolean modification = vi.analysis().getOrDefault(UNMODIFIED_VARIABLE, FALSE).isFalse();
                    boolean assignment = !vi.assignments().isEmpty();
                    if ((modification || assignment) && !methodInfo.isConstructor()) {
                        allFieldsUnmodified = false;
                    }
                    if (vi.isVariableInClosure()) {
                        VariableData vd = vi.variableInfoInClosure();
                        VariableInfo outerVi = vd.variableInfo(vi.variable().fullyQualifiedName());
                        try {
                            outerVi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE,
                                    ValueImpl.BoolImpl.from(!modification));
                        } catch (RuntimeException re) {
                            LOGGER.error("Overwrite error variable in closure {}", outerVi.variable());
                            throw re;
                        }
                    }
                }
            }
            if (!methodInfo.isConstructor()) {
                methodInfo.analysis().setAllowControlledOverwrite(NON_MODIFYING_METHOD,
                        ValueImpl.BoolImpl.from(allFieldsUnmodified));
            }
        }

        private void copyFromVariablesIntoMethodPi(VariableInfo vi, ParameterInfo pi) {
            Links links = vi.linkedVariables();
            if (vi.isUnmodified()) {
                // FIXME pi can still be indirectly modified IF it is modifiable and linked in a modifiable way to a field
                //
                pi.analysis().setAllowControlledOverwrite(UNMODIFIED_PARAMETER, TRUE);
            } // when linked to a field, we must wait for the field to be declared unmodified...

            // IMPORTANT: we also store this in case of !modified; see TestLinkCast,2
            // a parameter can be of type Object, not modified, even though, via casting, its hidden content is modified

            Value.VariableBooleanMap mfi = vi.analysis().getOrNull(MODIFIED_FI_COMPONENTS_VARIABLE,
                    ValueImpl.VariableBooleanMapImpl.class);
            if (mfi != null && !mfi.map().isEmpty()) {
                Value.VariableBooleanMap thisScope = translateVariableBooleanMapToThisScope(pi, mfi.map());
                pi.analysis().setAllowControlledOverwrite(MODIFIED_FI_COMPONENTS_PARAMETER, thisScope);
            }

            Value.SetOfTypeInfo casts = vi.analysis().getOrDefault(DOWNCAST_VARIABLE, ValueImpl.SetOfTypeInfoImpl.EMPTY);
            if (!casts.typeInfoSet().isEmpty()) {
                pi.analysis().setAllowControlledOverwrite(DOWNCAST_PARAMETER, casts);
            }
        }

        private Value.VariableBooleanMap translateVariableBooleanMapToThisScope(ParameterInfo pi, Map<Variable, Boolean> vbm) {
            This thisVar = runtime.newThis(pi.parameterizedType().typeInfo().asParameterizedType());
            Map<Variable, Boolean> map = vbm.entrySet().stream().collect(Collectors
                    .toUnmodifiableMap(e -> replaceScope(e.getKey(), thisVar), Map.Entry::getValue));
            return new ValueImpl.VariableBooleanMapImpl(map);
        }

        private Variable replaceScope(Variable v, Variable newScope) {
            Variable frScope = v.fieldReferenceBase();
            if (frScope != null) {
                TranslationMap tm = runtime.newTranslationMapBuilder().put(frScope, newScope).build();
                return tm.translateVariableRecursively(v);
            }
            return v;
        }
    }

    /*
    there is no real need to analyze per primary type
     */

    @Override
    public List<AnalyzerException> go(List<Info> analysisOrder) {
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(false);
        for (Info info : analysisOrder) {
            try {
                if (info instanceof MethodInfo mi) {
                    methodAnalyzer.doMethod(mi);
                }
            } catch (Exception | AssertionError problem) {
                LOGGER.error("Caught exception/error analyzing {}: {}", info, problem.getMessage());
                if (configuration.storeErrors()) {
                    methodAnalyzer.analyzerExceptions.add(new AnalyzerException(info, problem));
                    String errorMessage = Objects.requireNonNullElse(problem.getMessage(), "<no message>");
                    String fullMessage = "ANALYZER ERROR: " + errorMessage;
                    info.analysis().set(ANALYZER_ERROR, new ValueImpl.MessageImpl(fullMessage));
                } else {
                    throw problem;
                }
            }
        }
        return methodAnalyzer.analyzerExceptions;
    }
}
