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

public record MethodModification(Runtime runtime, VariableData variableData, Stage stage, MethodCall mc) {
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
        if (!receiverDisclaimed) {
            for (ParameterInfo pi : methodInfo.parameters()) {
                if (pi.isModified() && !pi.isIgnoreModifications()) {
                    if (pi.isVarArgs()) {
                        for (int i = methodInfo.parameters().size() - 1; i < mc.parameterExpressions().size(); i++) {
                            Result rp = params.get(i);
                            handleModifiedParameter(mc.parameterExpressions().get(i), rp, modified);
                        }
                    } else {
                        Result rp = params.get(pi.index());
                        handleModifiedParameter(mc.parameterExpressions().get(pi.index()), rp, modified);
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
        if (rp.links() != null && rp.links().primary() != null) {
            LOGGER.debug("Mark argument primary {} as modified by {}", rp.links().primary(), mc.methodInfo());
            Util.variableAndScopes(rp.links().primary()).forEach(modified::add);
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
