package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.*;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.Stage;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;

/*
convention:
The LINKS are written to each method, regardless of whether they're empty or not.
They act as a marker for computation as well.
Links of a parameter are only written when non-empty.

Synchronization: ensure that each thread runs in its own instance of this class, as we're not synchronizing.
Secondary synchronization takes place in PropertyValueMapImpl.getOrCreate().
 */

public class LinkComputerImpl implements LinkComputer, LinkComputerRecursion {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkComputerImpl.class);
    private final boolean forceShallow;
    private final JavaInspector javaInspector;
    private final RecursionPrevention recursionPrevention;
    private final ShallowMethodLinkComputer shallowMethodLinkComputer;

    public LinkComputerImpl(JavaInspector javaInspector) {
        this(javaInspector, true, false);
    }

    public LinkComputerImpl(JavaInspector javaInspector, boolean recurse, boolean forceShallow) {
        this.recursionPrevention = new RecursionPrevention(recurse);
        this.javaInspector = javaInspector;
        this.forceShallow = forceShallow;
        VirtualFieldComputer virtualFieldComputer = new VirtualFieldComputer(javaInspector);
        this.shallowMethodLinkComputer = new ShallowMethodLinkComputer(javaInspector.runtime(), virtualFieldComputer);
    }

    @Override
    public void doPrimaryType(TypeInfo primaryType) {
        doType(primaryType);
    }

    private void doType(TypeInfo typeInfo) {
        typeInfo.subTypes().forEach(this::doType);
        typeInfo.constructorAndMethodStream().forEach(mi -> mi.analysis().getOrCreate(METHOD_LINKS, () -> doMethod(mi)));
    }

    @Override
    public Links doField(FieldInfo fieldInfo) {
        throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void doAnonymousType(TypeInfo typeInfo) {
        doType(typeInfo);
    }

    @Override
    public MethodLinkedVariables doMethod(MethodInfo method) {
        MethodLinkedVariables alreadyDone = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        assert alreadyDone == null : "We should come from analysis(), we have just checked.";

        try {
            TypeInfo typeInfo = method.typeInfo();
            boolean shallow = forceShallow || method.isAbstract() || typeInfo.compilationUnit().externalLibrary();
            return doMethod(method, shallow, false);
        } catch (RuntimeException | AssertionError e) {
            LOGGER.error("Caught exception computing {}", method, e);
            throw e;
        }
    }

    @Override
    public MethodLinkedVariables recurseMethod(MethodInfo method) {
        MethodLinkedVariables alreadyDone = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        if (alreadyDone != null) return alreadyDone;
        try {
            TypeInfo typeInfo = method.typeInfo();
            boolean shallow = forceShallow || typeInfo.compilationUnit().externalLibrary();
            return doMethod(method, shallow, true);
        } catch (RuntimeException | AssertionError e) {
            LOGGER.error("Caught exception recursively computing {}", method, e);
            throw e;
        }
    }

    @Override
    public MethodLinkedVariables doMethodShallowDoNotWrite(MethodInfo methodInfo) {
        return doMethod(methodInfo, true, false);
    }

    private MethodLinkedVariables doMethod(MethodInfo methodInfo, boolean shallow, boolean write) {
        if (shallow) {
            MethodLinkedVariables mlv = shallowMethodLinkComputer.go(methodInfo);
            if (write) {
                methodInfo.analysis().set(METHOD_LINKS, mlv);
            }
            return mlv;
        }
        MethodLinkedVariables tlv;
        if (recursionPrevention.sourceAllowed(methodInfo)) {
            try {
                tlv = new SourceMethodComputer(methodInfo).go();
                if (write) {
                    methodInfo.analysis().set(METHOD_LINKS, tlv);
                }
            } finally {
                recursionPrevention.doneSource(methodInfo);
            }
        } else {
            // we're already analyzing methodInfo... so we return a shallow copy, not written out!
            LOGGER.debug("Fall-back to shallow for {}", methodInfo);
            tlv = doMethod(methodInfo, true, false);
            //fallbackToShallow.incrementAndGet();
        }
        return tlv;
    }

    public class SourceMethodComputer {
        final MethodInfo methodInfo;
        final ExpressionVisitor expressionVisitor;
        Links ofReturnValue;

        public SourceMethodComputer(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
            this.expressionVisitor = new ExpressionVisitor(javaInspector, LinkComputerImpl.this,
                    this, methodInfo, recursionPrevention, new AtomicInteger());
        }

        public MethodLinkedVariables go() {
            VariableData vd = doBlock(methodInfo.methodBody(), null);
            // ...
            List<Links> ofParameters = new ExpandParameterLinks(javaInspector.runtime()).go(methodInfo, vd);

            MethodLinkedVariables mlv = new MethodLinkedVariablesImpl(ofReturnValue, ofParameters);
            LOGGER.debug("Return source method {}: {}", methodInfo, mlv);
            return mlv;
        }

        VariableData doBlock(Block block, VariableData previousVd) {
            VariableData vd = previousVd;
            for (Statement statement : block.statements()) {
                if (statement instanceof Block b) {
                    // a block among the statements
                    vd = doBlock(b, vd);
                } else {
                    vd = doStatement(statement, vd);
                }
            }
            return vd;
        }

        public VariableData doStatement(Statement statement, VariableData previousVd) {
            Map<Variable, Links> linkedVariables = new HashMap<>();

            boolean evaluate;
            switch (statement) {
                case LocalVariableCreation lvc -> {
                    linkedVariables.putAll(handleLvc(lvc, previousVd));
                    evaluate = false;
                }
                case ForEachStatement fe -> {
                    evaluate = true;
                }
                default -> evaluate = true;
            }
            ExpressionVisitor.Result r;
            if (evaluate) {
                Expression expression = statement.expression();
                if (expression != null && !expression.isEmpty()) {
                    r = expressionVisitor.visit(expression, previousVd);
                    linkedVariables.putAll(r.extra().map());
                    linkedVariables.merge(r.links().primary(), r.links(), Links::merge);
                } else {
                    r = null;
                }
            } else {
                r = null;
            }
            VariableData vd = VariableDataImpl.of(statement);
            copyEvalIntoVariableData(linkedVariables, vd, previousVd);

            if (statement instanceof ReturnStatement && r != null) {
                ReturnVariable rv = new ReturnVariableImpl(methodInfo);
                ofReturnValue = new ExpandReturnValueLinks(javaInspector.runtime()).go(rv, r.links(), r.extra(), vd);
            }

            return vd;
        }

        private void copyEvalIntoVariableData(Map<Variable, Links> linkedVariables, VariableData vd, VariableData previousVd) {
            Map<Variable, Links> expanded = new ExpandLocal(javaInspector.runtime()).go(linkedVariables, previousVd);
            vd.variableInfoContainerStream().forEach(vic -> {
                VariableInfo vi = vic.getPreviousOrInitial();
                Links links = expanded.getOrDefault(vi.variable(), LinksImpl.EMPTY);
                if (!links.isEmpty() && vic.hasEvaluation()) {
                    VariableInfo eval = vic.best(Stage.EVALUATION);
                    try {
                        eval.analysis().set(LINKS, links);
                    } catch (IllegalArgumentException iae) {
                        LinkComputerImpl.this.recursionPrevention.report(methodInfo);
                        throw iae;
                    }
                }
            });
        }

        public Map<Variable, Links> handleLvc(LocalVariableCreation lvc, VariableData previousVd) {
            Map<Variable, Links> linkedVariables = new HashMap<>();
            lvc.localVariableStream().forEach(lv -> {
                if (!lv.assignmentExpression().isEmpty()) {
                    ExpressionVisitor.Result r = expressionVisitor.visit(lv.assignmentExpression(),
                            previousVd);
                    r.extra().forEach(e -> linkedVariables.put(e.getKey(), e.getValue()));
                    if (!r.links().isEmpty()) {
                        linkedVariables.put(lv, r.links().changePrimaryTo(javaInspector.runtime(), lv, null));
                    }
                    // FIXME we need this code, but not here
                    if (lv.assignmentExpression() instanceof VariableExpression ve &&
                        r.links().primary() != null && ve.parameterizedType().equals(lv.parameterizedType())) {
                        // make sure that we link the variables with '=='
                        linkedVariables.merge(lv,
                                new LinksImpl.Builder(lv)
                                        .add(LinkNature.IS_IDENTICAL_TO, r.links().primary())
                                        .build(),
                                Links::merge);
                    }
                }
            });
            return linkedVariables;
        }

    }
}
