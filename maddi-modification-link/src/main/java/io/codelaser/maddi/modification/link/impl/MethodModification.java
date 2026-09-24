package io.codelaser.maddi.modification.link.impl;

import io.codelaser.maddi.modification.link.impl.translate.VariableTranslationMap;
import io.codelaser.maddi.modification.prepwork.Util;
import io.codelaser.maddi.modification.prepwork.variable.MethodLinkedVariables;
import io.codelaser.maddi.modification.prepwork.variable.Stage;
import io.codelaser.maddi.modification.prepwork.variable.VariableData;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @param currentMethod           the method whose body contains {@code mc}
 * @param throughPassedFunction   receives (parameter, entry) when a parameter of {@code currentMethod} is handed to
 *                                one of its own functional parameters: recorded instead of marked modified, see
 *                                {@link PassedFunction}
 */
public record MethodModification(Runtime runtime, VariableData variableData, Stage stage, MethodCall mc,
                                 MethodInfo currentMethod,
                                 BiConsumer<ParameterInfo, String> throughPassedFunction) {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodModification.class);

    public Set<Variable> go(Variable objectPrimary, List<Result> params, MethodLinkedVariables methodLinkedVariables) {
        Set<Variable> modified = new HashSet<>();
        MethodInfo methodInfo = mc.methodInfo();
        // The RECEIVER carries @IgnoreModifications: the author has declared that whatever this object does is
        // not their modification. That must cover what it does to the ARGUMENTS it is handed, not only the
        // object itself -- `f.apply(this._1, this._2)` must not mark `this._1` modified when `f` is disclaimed.
        // Without this, the disclaimer reached only the receiver (just below), while the decision about
        // arguments was taken solely from the CALLEE's declared parameters -- for a functional interface that is
        // java.util.function.Function.apply(@Modified T t) in the JDK hints, which nothing at the call site can
        // influence. Measured on vavr 2026-09-22: @IgnoreModifications on all five of io.vavr.Tuple2's function
        // parameters set the six ignoreModsParameter properties and changed nothing else, because of exactly
        // this. The codebase's own motivating example, Element.visit(@IgnoreModifications Predicate p) calling
        // p.test(this), has the same shape and worked only because Predicate.test's ARGUMENT was moved to
        // @NotModified on 2026-07-23 -- fixed on the callee side because the receiver's disclaimer did not reach.
        boolean receiverDisclaimed = objectPrimary != null
                                     && Util.variableAndScopes(objectPrimary).anyMatch(Variable::isIgnoreModifications);
        if (objectPrimary != null && !methodInfo.isFinalizer()) {
            if (methodInfo.isModifying() && !methodInfo.isIgnoreModification()) {
                LOGGER.debug("Mark object primary {} as modified by {}", objectPrimary, methodInfo);
                Util.variableAndScopes(objectPrimary)
                        .filter(v -> !v.isIgnoreModifications())
                        .forEach(modified::add);
            }
        }
        // the receiver is one of OUR functional parameters and this is its SAM: what it does to its arguments
        // depends on the function our caller passes (PassedFunction)
        ParameterInfo functionalReceiver = objectPrimary instanceof ParameterInfo fp
                                           && fp.methodInfo() == currentMethod
                                           && methodInfo.isAbstract()
                                           && fp.parameterizedType().isFunctionalInterface() ? fp : null;
        Set<Variable> recordedThroughPassedFunction = new HashSet<>();
        if (!receiverDisclaimed) {
            for (ParameterInfo pi : methodInfo.parameters()) {
                if (pi.isModified() && !pi.isIgnoreModifications()) {
                    if (pi.isVarArgs()) {
                        for (int i = methodInfo.parameters().size() - 1; i < mc.parameterExpressions().size(); i++) {
                            Result rp = params.get(i);
                            handleModifiedParameter(mc.parameterExpressions().get(i), rp, modified);
                        }
                    } else if (PassedFunction.unmodifiedAtCallSite(pi, mc.parameterExpressions())) {
                        LOGGER.debug("Argument {} of {} not modified: the function passed leaves it alone", pi.index(),
                                methodInfo);
                    } else {
                        Result rp = params.get(pi.index());
                        if (functionalReceiver != null && throughPassedFunction != null
                            && rp.links() != null && rp.links().isEmpty()
                            && rp.links().primary() instanceof ParameterInfo ownParameter
                            && ownParameter.methodInfo() == currentMethod) {
                            throughPassedFunction.accept(ownParameter,
                                    PassedFunction.entry(functionalReceiver.index(), pi.index()));
                            recordedThroughPassedFunction.add(ownParameter);
                        } else {
                            handleModifiedParameter(mc.parameterExpressions().get(pi.index()), rp, modified);
                        }
                    }
                }
            }
        }
        // the same reasoning for the callee's OWN propagated modifications: if none of this call's effects are
        // the author's, that includes the ones the callee's body performs on variables translated into our scope
        if (!receiverDisclaimed && !methodLinkedVariables.isEmpty() && objectPrimary != null) {
            Variable methodThis = runtime.newThis(methodInfo.typeInfo().asParameterizedType());
            TranslationMap tm = new VariableTranslationMap(runtime).put(methodThis, objectPrimary);
            for (Variable mv : methodLinkedVariables.modified()) {
                Variable translated = tm.translateVariableRecursively(mv);
                // the SAM's own summary says its argument is modified: for a recorded parameter, that is the same
                // conditional modification, not a second one
                if (recordedThroughPassedFunction.contains(translated)) continue;
                if (translated.equals(mv)
                    || variableData != null && variableData.isKnown(translated.fullyQualifiedName())) {
                    LOGGER.debug("Propagated modification to {}", translated);
                    Util.variableAndScopes(translated)
                            .filter(v -> !v.isIgnoreModifications())
                            .forEach(modified::add);
                }
            }
        }
        return modified;
    }

    private void handleModifiedParameter(Expression argument, Result rp, Set<Variable> modified) {
        if (rp.links() != null && rp.links().primary() != null && !Util.isHiddenContentField(rp.links().primary())) {
            LOGGER.debug("Mark argument primary {} as modified by {}", rp.links().primary(), mc.methodInfo());
            // the LAST of go()'s four modification-recording sites to get this filter (fix C, 2026-09-22): a
            // disclaimed face never implicates its own node, whichever site reaches it. Handing a field that is
            // @IgnoreModifications to a callee's @Modified parameter is the author saying that what the callee
            // does to it is not this type's modification -- exactly what the receiver and propagated sites
            // already honoured. ShadowModificationPass.project() has ALWAYS filtered it here (its FieldReference
            // arm), so before this the two implementations disagreed on the argument site by construction.
            Util.variableAndScopes(rp.links().primary())
                    .filter(v -> !v.isIgnoreModifications())
                    .forEach(modified::add);
        }
        if (argument instanceof MethodReference mr) {
            propagateModificationOfObject(modified, mr);
        }
    }

    private void propagateModificationOfObject(Set<Variable> modified, MethodReference mr) {
        if (mr.methodInfo().isModifying() && mr.scope() instanceof VariableExpression ve) {
            Util.variableAndScopes(ve.variable())
                    .filter(v -> !v.isIgnoreModifications())
                    .forEach(modified::add);
        }
    }
}
