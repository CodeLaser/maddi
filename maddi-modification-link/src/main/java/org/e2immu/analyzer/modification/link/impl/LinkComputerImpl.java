package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.DOWNCAST_VARIABLE;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

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
        final Set<Variable> erase = new HashSet<>();
        Links ofReturnValue;
        final Set<LocalVariable> variablesRepresentingConstants = new HashSet<>();

        public SourceMethodComputer(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
            this.expressionVisitor = new ExpressionVisitor(javaInspector, new VirtualFieldComputer(javaInspector),
                    LinkComputerImpl.this, this, methodInfo, recursionPrevention,
                    new AtomicInteger());
        }

        private TranslationMap replaceConstants(VariableData vd, Stage stage, ExpressionVisitor.Result r) {
            TranslateConstants tc = new TranslateConstants(javaInspector.runtime());
            for (LocalVariable lv : variablesRepresentingConstants) {
                tc.put(lv, lv.assignmentExpression());
            }
            if (vd != null) {
                vd.variableInfoStream(stage).forEach(vi -> {
                    if (vi.linkedVariables() != null) {
                        vi.linkedVariables().stream()
                                .filter(l -> l.from().equals(vi.variable())
                                             && l.linkNature().isIdenticalTo()
                                             && l.to() instanceof LocalVariable)
                                .map(l -> tc.get(l.to()))
                                .filter(Objects::nonNull)
                                .findFirst()
                                .ifPresent(constant -> tc.put(vi.variable(), constant));
                    }
                });
            }
            return tc;
        }

        public MethodLinkedVariables go() {
            VariableData vd = doBlock(methodInfo.methodBody(), null);
            List<Links> ofParameters = expand.parameters(methodInfo, vd, replaceConstants(vd, Stage.MERGE, null));

            MethodLinkedVariables mlv = new MethodLinkedVariablesImpl(ofReturnValue, ofParameters);
            LOGGER.debug("Return source method {}: {}", methodInfo, mlv);
            return mlv;
        }

        VariableData doBlock(Block block, VariableData previousVd) {
            VariableData vd = previousVd;
            boolean first = true;
            for (Statement statement : block.statements()) {
                if (statement instanceof Block b) {
                    // a block among the statements
                    vd = doBlock(b, vd);
                } else {
                    vd = doStatement(statement, vd, first);
                }
                first = false;
            }
            return vd;
        }

        public VariableData doStatement(Statement statement, VariableData previousVd, boolean first) {
            LOGGER.debug("Do statement {} {}", methodInfo.fullyQualifiedName(), statement.source().index());

            Stage stageOfPrevious = first ? Stage.EVALUATION : Stage.MERGE;
            LocalVariable forEachLv;
            boolean evaluate;

            ExpressionVisitor.Result r;
            switch (statement) {
                case LocalVariableCreation lvc -> {
                    r = handleLvc(lvc, previousVd, stageOfPrevious);
                    evaluate = false;
                    forEachLv = null;
                }
                case ForEachStatement fe -> {
                    r = handleLvc(fe.initializer(), previousVd, stageOfPrevious);
                    forEachLv = fe.initializer().localVariable();
                    evaluate = false;
                }
                case ForStatement fs -> {
                    r = fs.initializers().stream()
                            .filter(i -> i instanceof LocalVariableCreation)
                            .map(i ->
                                    handleLvc((LocalVariableCreation) i, previousVd, stageOfPrevious))
                            .reduce(ExpressionVisitor.EMPTY, ExpressionVisitor.Result::merge);
                    evaluate = true;
                    forEachLv = null;
                }
                default -> {
                    evaluate = true;
                    forEachLv = null;
                    r = null;
                }
            }
            if (evaluate) {
                Expression expression = statement.expression();
                if (expression != null && !expression.isEmpty()) {
                    Expression e = javaInspector.runtime().sortAndSimplify(true, expression);
                    ExpressionVisitor.Result u = expressionVisitor.visit(e, previousVd, stageOfPrevious);
                    r = r == null ? u : r.merge(u);
                }
            }
            if (forEachLv != null) {
                Expression e = javaInspector.runtime().sortAndSimplify(true, statement.expression());
                ExpressionVisitor.Result u = new ForEach(javaInspector.runtime(), expressionVisitor)
                        .linkIntoIterable(forEachLv.parameterizedType(), e, previousVd, stageOfPrevious);
                Links newLinks = new LinksImpl.Builder(forEachLv)
                        .add(LinkNatureImpl.IS_ASSIGNED_FROM, u.links().primary())
                        .build();
                r = u.addExtra(Map.of(forEachLv, newLinks));
            }
            if (statement instanceof ForStatement fs) {
                for (Expression updater : fs.updaters()) {
                    Expression sorted = javaInspector.runtime().sortAndSimplify(true, updater);
                    ExpressionVisitor.Result u = expressionVisitor.visit(sorted, previousVd, stageOfPrevious);
                    r = r == null ? u : r.merge(u);
                }
            }
            TranslationMap replaceConstants;
            if (r != null) {
                this.variablesRepresentingConstants.addAll(r.variablesRepresentingConstants());
                replaceConstants = replaceConstants(previousVd, stageOfPrevious, r);
            } else {
                replaceConstants = null;
            }
            VariableData vd = VariableDataImpl.of(statement);
            if (r != null) {
                r = r.copyLinksToExtra();
            }
            if (r != null) {
                this.erase.addAll(r.erase());
                copyEvalIntoVariableData(r, previousVd, stageOfPrevious, vd, replaceConstants);
                if (!r.casts().isEmpty()) {
                    writeCasts(r.casts(), previousVd, stageOfPrevious, vd);
                }
            }
            if (statement.hasSubBlocks()) {
                handleSubBlocks(statement, vd, r);
            }

            if (statement instanceof ReturnStatement && r != null && r.links() != null) {
                ReturnVariable rv = new ReturnVariableImpl(methodInfo);
                ofReturnValue = expand.returnValue(rv, r.links(), r.extra(), vd, replaceConstants);
                VariableInfoImpl vii = ((VariableInfoImpl) vd.variableInfo(rv));
                vii.setLinkedVariables(ofReturnValue);
            }
            if (r != null) {
                writeOutMethodCallAnalysis(r.writeMethodCalls(), vd);
            }
            return vd;
        }

        private void writeCasts(Map<Variable, Set<TypeInfo>> casts,
                                VariableData previous,
                                Stage stageOfPrevious,
                                VariableData variableData) {
            if (previous != null) {
                previous.variableInfoStream(stageOfPrevious).forEach(vi -> {
                    if (variableData.isKnown(vi.variable().fullyQualifiedName())) {
                        Value.SetOfTypeInfo set = vi.analysis().getOrDefault(VariableInfoImpl.DOWNCAST_VARIABLE,
                                ValueImpl.SetOfTypeInfoImpl.EMPTY);
                        if (!set.typeInfoSet().isEmpty()) {
                            casts.computeIfAbsent(vi.variable(), _ -> new HashSet<>()).addAll(set.typeInfoSet());
                        }
                    }
                });
            }
            casts.forEach((v, set) -> {
                VariableInfoContainer vic = variableData.variableInfoContainerOrNull(v.fullyQualifiedName());
                VariableInfoImpl vii = (VariableInfoImpl) vic.best(Stage.EVALUATION);
                vii.analysis().setAllowControlledOverwrite(VariableInfoImpl.DOWNCAST_VARIABLE,
                        new ValueImpl.SetOfTypeInfoImpl(Set.copyOf(set)));
            });
        }

        private void writeOutMethodCallAnalysis(List<ExpressionVisitor.WriteMethodCall> writeMethodCalls,
                                                VariableData vd) {
            for (ExpressionVisitor.WriteMethodCall wmc : writeMethodCalls) {
                // rather than taking the links in wmc, we want the fully expanded links
                // (after running copyEvalIntoVariableData)
                Map<Variable, Boolean> variablesLinkedToObject = new HashMap<>();
                for (Link l : wmc.linksFromObject()) {
                    addToVariablesLinkedToObject(vd, l.to(), variablesLinkedToObject);
                }
                addToVariablesLinkedToObject(vd, wmc.linksFromObject().primary(), variablesLinkedToObject);
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

        private static void addToVariablesLinkedToObject(VariableData vd,
                                                         Variable sub,
                                                         Map<Variable, Boolean> variablesLinkedToObject) {
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

        private void handleSubBlocks(Statement statement, VariableData vd, ExpressionVisitor.Result r) {
            List<VariableData> vds = statement.subBlockStream()
                    .filter(block -> !block.isEmpty())
                    .map(block -> doBlock(block, vd))
                    .filter(Objects::nonNull)
                    .toList();
            vd.variableInfoContainerStream()
                    .filter(VariableInfoContainer::hasMerge)
                    .forEach(vic -> {
                        VariableInfo best = vic.best(Stage.EVALUATION);
                        Variable variable = best.variable();
                        Links eval = r == null ? null : r.extra().map().get(variable);
                        String fqn = variable.fullyQualifiedName();
                        Links.Builder collect = eval == null ? new LinksImpl.Builder(variable)
                                : new LinksImpl.Builder(eval);
                        AtomicBoolean unmodified = new AtomicBoolean(true);
                        Set<TypeInfo> downcasts = new HashSet<>(best.analysis().getOrDefault(DOWNCAST_VARIABLE,
                                ValueImpl.SetOfTypeInfoImpl.EMPTY).typeInfoSet());
                        vds.forEach(subVd -> {
                            VariableInfoContainer subVic = subVd.variableInfoContainerOrNull(fqn);
                            if (subVic != null) {
                                VariableInfo subVi = subVic.best();
                                Links subTlv = subVi.linkedVariables();
                                if (subTlv != null && subTlv.primary() != null) {
                                    collect.addAll(subTlv);
                                }
                                Value.Bool subUnmodified = subVi.analysis().getOrNull(UNMODIFIED_VARIABLE,
                                        ValueImpl.BoolImpl.class);
                                boolean explicitlyModified = subUnmodified != null && subUnmodified.isFalse();
                                if (explicitlyModified) unmodified.set(false);

                                Value.SetOfTypeInfo subDowncasts = subVi.analysis().getOrDefault(DOWNCAST_VARIABLE,
                                        ValueImpl.SetOfTypeInfoImpl.EMPTY);
                                downcasts.addAll(subDowncasts.typeInfoSet());
                            }
                        });
                        assert vic.hasMerge();
                        VariableInfoImpl merge = (VariableInfoImpl) vic.best();
                        Links collected = collect.build();
                        if (!collected.isEmpty()) {
                            merge.setLinkedVariables(collected);
                        }
                        merge.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE,
                                ValueImpl.BoolImpl.from(unmodified.get()));
                        if (!downcasts.isEmpty()) {
                            merge.analysis().setAllowControlledOverwrite(DOWNCAST_VARIABLE,
                                    new ValueImpl.SetOfTypeInfoImpl(Set.copyOf(downcasts)));
                        }
                    });
        }

        private void copyEvalIntoVariableData(ExpressionVisitor.Result r,
                                              VariableData previousVd,
                                              Stage stageOfPrevious,
                                              VariableData vd,
                                              TranslationMap replaceConstants) {
            Set<Variable> modifiedDuringEvaluation = r == null ? Set.of() : r.modified();
            Map<Variable, Links> linkedVariables = r == null ? Map.of() : r.extra().map();
            Map<Variable, Links> expanded = expand.local(linkedVariables, modifiedDuringEvaluation, previousVd,
                    stageOfPrevious, vd, replaceConstants);
            vd.variableInfoContainerStream().forEach(vic -> {
                VariableInfo vi = vic.getPreviousOrInitial();
                Links links;
                if (this.erase.contains(vi.variable())
                    || !Collections.disjoint(Util.scopeVariables(vi.variable()), this.erase)) {
                    links = new LinksImpl(vi.variable());
                } else {
                    links = expanded.getOrDefault(vi.variable(), LinksImpl.EMPTY);
                }
                if (vic.hasEvaluation()) {
                    VariableInfoImpl eval = (VariableInfoImpl) vic.best(Stage.EVALUATION);
                    eval.setLinkedVariables(links);
                }
            });
        }

        public ExpressionVisitor.Result handleLvc(LocalVariableCreation lvc,
                                                  VariableData previousVd,
                                                  Stage stageOfPrevious) {
            return lvc.localVariableStream()
                    .map(lv -> handleSingleLvc(previousVd, stageOfPrevious, lv))
                    .reduce(ExpressionVisitor.EMPTY, ExpressionVisitor.Result::merge);
        }

        private ExpressionVisitor.@NotNull Result handleSingleLvc(VariableData previousVd,
                                                                  Stage stageOfPrevious,
                                                                  LocalVariable lv) {
            ExpressionVisitor.Result r;
            if (!lv.assignmentExpression().isEmpty()) {
                Map<Variable, Links> linkedVariables = new HashMap<>();
                Expression e = javaInspector.runtime().sortAndSimplify(true, lv.assignmentExpression());
                r = expressionVisitor.visit(e, previousVd, stageOfPrevious);
                //r.extra().forEach(e -> linkedVariables.put(e.getKey(), e.getValue()));
                if (!r.links().isEmpty()) {
                    linkedVariables.put(lv, r.links().changePrimaryTo(javaInspector.runtime(), lv));
                }
                if (r.links().primary() != null) {
                    // make sure that we link the variables with '==', as we do in ExpressionVisitor.assignment
                    Links newLinks = new LinksImpl.Builder(lv)
                            .add(LinkNatureImpl.IS_ASSIGNED_FROM, r.links().primary())
                            .build();
                    linkedVariables.merge(lv, newLinks, Links::merge);
                }
                return r.addExtra(linkedVariables);
            }
            return ExpressionVisitor.EMPTY;
        }
    }
}
