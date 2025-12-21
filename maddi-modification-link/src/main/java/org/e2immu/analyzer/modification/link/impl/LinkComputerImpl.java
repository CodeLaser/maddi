package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static final PropertyImpl VARIABLES_LINKED_TO_OBJECT = new PropertyImpl("variablesLinkedToObject",
            ValueImpl.VariableBooleanMapImpl.EMPTY);

    private final boolean forceShallow;
    private final JavaInspector javaInspector;
    private final RecursionPrevention recursionPrevention;
    private final ShallowMethodLinkComputer shallowMethodLinkComputer;
    private final Expand expand;

    public LinkComputerImpl(JavaInspector javaInspector) {
        this(javaInspector, true, false);
    }

    public LinkComputerImpl(JavaInspector javaInspector, boolean recurse, boolean forceShallow) {
        this.recursionPrevention = new RecursionPrevention(recurse);
        this.javaInspector = javaInspector;
        this.forceShallow = forceShallow;
        VirtualFieldComputer virtualFieldComputer = new VirtualFieldComputer(javaInspector);
        this.shallowMethodLinkComputer = new ShallowMethodLinkComputer(javaInspector.runtime(), virtualFieldComputer);
        this.expand = new Expand(javaInspector.runtime());
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
        }
        return tlv;
    }

    public class SourceMethodComputer {
        final MethodInfo methodInfo;
        final ExpressionVisitor expressionVisitor;
        Links ofReturnValue;

        public SourceMethodComputer(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
            this.expressionVisitor = new ExpressionVisitor(javaInspector, new VirtualFieldComputer(javaInspector),
                    LinkComputerImpl.this, this, methodInfo, recursionPrevention,
                    new AtomicInteger());
        }

        public MethodLinkedVariables go() {
            VariableData vd = doBlock(methodInfo.methodBody(), null);
            // ...
            List<Links> ofParameters = expand.parameters(methodInfo, vd);

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
            List<ExpressionVisitor.WriteMethodCall> writeMethodCalls = new LinkedList<>();
            LocalVariable forEachLv;
            boolean evaluate;

            switch (statement) {
                case LocalVariableCreation lvc -> {
                    linkedVariables.putAll(handleLvc(lvc, previousVd, writeMethodCalls));
                    evaluate = false;
                    forEachLv = null;
                }
                case ForEachStatement fe -> {
                    handleLvc(fe.initializer(), previousVd, writeMethodCalls);
                    forEachLv = fe.initializer().localVariable();
                    evaluate = false;
                }
                case ForStatement fs -> {
                    fs.initializers().stream()
                            .filter(i -> i instanceof LocalVariableCreation)
                            .forEach(i ->
                                    handleLvc((LocalVariableCreation) i, previousVd, writeMethodCalls));
                    evaluate = true;
                    forEachLv = null;
                }
                default -> {
                    evaluate = true;
                    forEachLv = null;
                }
            }
            ExpressionVisitor.Result r;
            if (evaluate) {
                Expression expression = statement.expression();
                if (expression != null && !expression.isEmpty()) {
                    r = expressionVisitor.visit(expression, previousVd);
                    linkedVariables.putAll(r.extra().map());
                    if (r.links().primary() != null) {
                        linkedVariables.merge(r.links().primary(), r.links(), Links::merge);
                    }
                    writeMethodCalls.addAll(r.writeMethodCalls());
                } else {
                    r = null;
                }
            } else {
                r = null;
            }
            if (forEachLv != null) {
                ExpressionVisitor.Result r2 = new ForEach(javaInspector.runtime(), expressionVisitor)
                        .linkIntoIterable(forEachLv.parameterizedType(), statement.expression(), previousVd);
                linkedVariables.put(r2.links().primary(), r2.links());
                r2.extra().forEach(e -> linkedVariables.merge(e.getKey(), e.getValue(), Links::merge));
                linkedVariables.put(forEachLv, new LinksImpl.Builder(forEachLv).add(LinkNature.IS_IDENTICAL_TO, r2.links().primary()).build());
            }
            if (statement instanceof ForStatement fs) {
                fs.updaters().forEach(updater -> {
                    ExpressionVisitor.Result u = expressionVisitor.visit(updater, previousVd);
                    // FIXME merge result into ...?
                });
            }
            VariableData vd = VariableDataImpl.of(statement);
            copyEvalIntoVariableData(linkedVariables, r == null ? Set.of() : r.modified(), previousVd, vd);
            if (statement.hasSubBlocks()) {
                handleSubBlocks(statement, vd, linkedVariables);
            }

            if (statement instanceof ReturnStatement && r != null) {
                ReturnVariable rv = new ReturnVariableImpl(methodInfo);
                ofReturnValue = expand.returnValue(rv, r.links(), r.extra(), vd);
                VariableInfoImpl vii = ((VariableInfoImpl) vd.variableInfo(rv));
                vii.setLinkedVariables(ofReturnValue);
            }
            writeOutMethodCallAnalysis(writeMethodCalls, vd);
            return vd;
        }

        private void writeOutMethodCallAnalysis(List<ExpressionVisitor.WriteMethodCall> writeMethodCalls, VariableData vd) {
            for (ExpressionVisitor.WriteMethodCall wmc : writeMethodCalls) {
                // rather than taking the links in wmc, we want the fully expanded links
                // (after running copyEvalIntoVariableData)
                Map<Variable, Boolean> variablesLinkedToObject = new HashMap<>();
                for (Link l : wmc.linksFromObject()) {
                    addToVlto(vd, l.to(), variablesLinkedToObject);
                }
                addToVlto(vd, wmc.linksFromObject().primary(), variablesLinkedToObject);
                if (!variablesLinkedToObject.isEmpty()) {
                    try {
                        wmc.methodCall().analysis().set(VARIABLES_LINKED_TO_OBJECT,
                                new ValueImpl.VariableBooleanMapImpl(Map.copyOf(variablesLinkedToObject)));
                    } catch (IllegalArgumentException iae) {
                        LinkComputerImpl.this.recursionPrevention.report(methodInfo);
                        throw iae;
                    }
                }
            }
        }

        private static void addToVlto(VariableData vd, Variable sub, Map<Variable, Boolean> variablesLinkedToObject) {
            Variable primary = Util.primary(sub);
            if (primary != null && vd.isKnown(primary.fullyQualifiedName())) {
                variablesLinkedToObject.put(primary, true);
                VariableInfo viPrimary = vd.variableInfo(primary);
                Links links = Objects.requireNonNullElse(viPrimary.linkedVariables(), LinksImpl.EMPTY);
                for (Link link : links) {
                    for (Variable v : org.e2immu.analyzer.modification.prepwork.Util.goUp(link.from())) {
                        if (vd.isKnown(v.fullyQualifiedName())) {
                            variablesLinkedToObject.put(v, true);
                        }
                    }
                    for (Variable v : org.e2immu.analyzer.modification.prepwork.Util.goUp(link.to())) {
                        if (vd.isKnown(v.fullyQualifiedName())) {
                            variablesLinkedToObject.put(v, false);
                        }
                    }

                }
            }
        }

        private void handleSubBlocks(Statement statement, VariableData vd, Map<Variable, Links> linkedVariables) {
            List<VariableData> vds = statement.subBlockStream().map(block -> doBlock(block, vd))
                    .filter(Objects::nonNull)
                    .toList();
            vd.variableInfoContainerStream()
                    .filter(VariableInfoContainer::hasMerge)
                    .forEach(vic -> {
                        VariableInfo best = vic.best(Stage.EVALUATION);
                        Variable variable = best.variable();
                        Links eval = linkedVariables.get(variable);
                        String fqn = variable.fullyQualifiedName();
                        Links.Builder collect = eval == null ? new LinksImpl.Builder(variable)
                                : new LinksImpl.Builder(eval);
                        vds.forEach(subVd -> {
                            VariableInfoContainer subVic = subVd.variableInfoContainerOrNull(fqn);
                            if (subVic != null) {
                                VariableInfo subVi = subVic.best();
                                Links subTlv = subVi.linkedVariables();
                                if (subTlv != null) {
                                    collect.addAll(subTlv);
                                }
                            }
                        });
                        Links collected = collect.build();
                        if (!collected.isEmpty()) {
                            VariableInfoImpl merge = (VariableInfoImpl) vic.best();
                            merge.setLinkedVariables(collected);  // FIXME allow overwrite
                        }
                    });
        }

        private void copyEvalIntoVariableData(Map<Variable, Links> linkedVariables,
                                              Set<Variable> modifiedDuringEvaluation,
                                              VariableData previousVd,
                                              VariableData vd) {
            Map<Variable, Links> expanded = expand.local(linkedVariables, modifiedDuringEvaluation, previousVd, vd);
            vd.variableInfoContainerStream().forEach(vic -> {
                VariableInfo vi = vic.getPreviousOrInitial();
                Links links = expanded.getOrDefault(vi.variable(), LinksImpl.EMPTY);
                if (vic.hasEvaluation()) {
                    VariableInfoImpl eval = (VariableInfoImpl) vic.best(Stage.EVALUATION);
                    eval.setLinkedVariables(links);
                }
            });
        }

        public Map<Variable, Links> handleLvc(LocalVariableCreation lvc, VariableData previousVd,
                                              List<ExpressionVisitor.WriteMethodCall> writeMethodCalls) {
            Map<Variable, Links> linkedVariables = new HashMap<>();
            lvc.localVariableStream().forEach(lv -> {
                if (!lv.assignmentExpression().isEmpty()) {
                    ExpressionVisitor.Result r = expressionVisitor.visit(lv.assignmentExpression(), previousVd);
                    r.extra().forEach(e -> linkedVariables.put(e.getKey(), e.getValue()));
                    writeMethodCalls.addAll(r.writeMethodCalls());

                    if (!r.links().isEmpty()) {
                        linkedVariables.put(lv, r.links().changePrimaryTo(javaInspector.runtime(), lv));
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
