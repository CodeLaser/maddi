package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
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
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final Options options;
    private final JavaInspector javaInspector;
    private final RecursionPrevention recursionPrevention;
    private final ShallowMethodLinkComputer shallowMethodLinkComputer;
    private final LinkGraph linkGraph;
    private final WriteLinksAndModification writeLinksAndModification;

    // for testing :-) especially duplicate name checking
    public LinkComputerImpl(JavaInspector javaInspector) {
        this(javaInspector, Options.TEST);
    }

    public LinkComputerImpl(JavaInspector javaInspector, Options options) {
        this.recursionPrevention = new RecursionPrevention(options.recurse());
        this.javaInspector = javaInspector;
        this.options = options;
        VirtualFieldComputer virtualFieldComputer = new VirtualFieldComputer(javaInspector);
        this.shallowMethodLinkComputer = new ShallowMethodLinkComputer(javaInspector.runtime(), virtualFieldComputer);
        this.linkGraph = new LinkGraph(javaInspector, javaInspector.runtime(), options.checkDuplicateNames());
        this.writeLinksAndModification = new WriteLinksAndModification(javaInspector, javaInspector.runtime());
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
            boolean shallow = options.forceShallow() || method.isAbstract() || typeInfo.compilationUnit().externalLibrary();
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
            boolean shallow = options.forceShallow() || typeInfo.compilationUnit().externalLibrary();
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
        final Set<LocalVariable> variablesRepresentingConstants = new HashSet<>();
        final Variable returnVariable;
        final Set<Variable> modificationsOutsideVariableData = new HashSet<>();

        public SourceMethodComputer(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
            this.expressionVisitor = new ExpressionVisitor(javaInspector.runtime(),
                    javaInspector, options, new VirtualFieldComputer(javaInspector),
                    LinkComputerImpl.this, this, methodInfo, recursionPrevention,
                    new AtomicInteger());
            this.returnVariable = methodInfo.hasReturnValue() ? new ReturnVariableImpl(methodInfo) : null;
        }

        private TranslationMap replaceConstants(VariableData vd, Stage stage) {
            TranslateConstants tc = new TranslateConstants(javaInspector.runtime());
            for (LocalVariable lv : variablesRepresentingConstants) {
                tc.put(lv, lv.assignmentExpression());
            }
            if (vd != null) {
                vd.variableInfoStream(stage).forEach(vi -> {
                    if (vi.linkedVariables() != null) {
                        vi.linkedVariables().stream()
                                .filter(l -> l.from().equals(vi.variable())
                                             && l.linkNature().isIdenticalToOrAssignedFromTo()
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
            Links ofReturnValue = vd == null || returnVariable == null
                                  || !vd.isKnown(returnVariable.fullyQualifiedName())
                    ? LinksImpl.EMPTY
                    : emptyIfOnlySomeValue(vd.variableInfo(returnVariable).linkedVariables());
            Set<ParameterInfo> paramsInOfReturnValue = ofReturnValue.stream()
                    .flatMap(Link::parameterStream)
                    .collect(Collectors.toUnmodifiableSet());
            List<Links> ofParameters = methodInfo.parameters().stream()
                    .map(pi -> filteredPi(pi, paramsInOfReturnValue, vd)).toList();
            Set<Variable> modified = vd == null ? Set.of()
                    : vd.variableInfoStream()
                    .filter(vi -> !vi.variable().equals(returnVariable)
                                  && LinkVariable.acceptForLinkedVariables(vi.variable()))
                    .filter(VariableInfo::isModified)
                    .map(VariableInfo::variable)
                    .collect(Collectors.toUnmodifiableSet());
            Set<Variable> modifiedOutside = this.modificationsOutsideVariableData.stream()
                    .filter(LinkVariable::acceptForLinkedVariables)
                    .collect(Collectors.toUnmodifiableSet());
            Set<Variable> allModified = Stream.concat(modified.stream(), modifiedOutside.stream())
                    .collect(Collectors.toUnmodifiableSet());
            MethodLinkedVariables mlv = new MethodLinkedVariablesImpl(ofReturnValue, ofParameters, allModified);
            copyModificationsIntoMethod(modified);
            LOGGER.debug("Return source method {}: {}", methodInfo, mlv);
            return mlv;
        }

        private void copyModificationsIntoMethod(Set<Variable> modified) {
            boolean methodModified = false;
            boolean[] paramsModified = new boolean[methodInfo.parameters().size()];
            for (Variable v : modified) {
                if (v instanceof This thisVar && thisVar.typeInfo().equals(methodInfo.typeInfo())) {
                    methodModified = true;
                } else if (v instanceof ParameterInfo pi && pi.methodInfo().equals(methodInfo)) {
                    paramsModified[pi.index()] = true;
                }
            }
            methodInfo.analysis().set(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.from(!methodModified));
            for (ParameterInfo pi : methodInfo.parameters()) {
                pi.analysis().set(PropertyImpl.UNMODIFIED_PARAMETER, ValueImpl.BoolImpl.from(!paramsModified[pi.index()]));
            }
        }


        private Links emptyIfOnlySomeValue(Links links) {
            if (links.stream().allMatch(l ->
                    l.to() instanceof MarkerVariable mv && (mv.isSomeValue() || mv.isConstant()))) {
                return LinksImpl.EMPTY;
            }
            // remove SomeValue unless p0 is present
            if (!links.isEmpty()) {
                ParameterInfo p0 = methodInfo.parameters().isEmpty() ? null : methodInfo.parameters().getFirst();
                if (p0 == null || links.stream().noneMatch(l ->
                        l.from().equals(links.primary()) &&
                        l.linkNature().isIdenticalToOrAssignedFromTo() &&
                        l.to() instanceof ParameterInfo pi && pi.equals(p0))) {
                    return links.removeIfTo(v -> v instanceof MarkerVariable mv && mv.isSomeValue());
                }
            }
            return links;
        }

        private static Links filteredPi(ParameterInfo pi, Set<ParameterInfo> ignoreReturnValue, VariableData vd) {
            if (vd == null) return LinksImpl.EMPTY;
            VariableInfoContainer vic = vd.variableInfoContainerOrNull(pi.fullyQualifiedName());
            if (vic == null) return LinksImpl.EMPTY;
            VariableInfo vi = vic.best();
            Links viLinks = vi.linkedVariables();
            if (viLinks == null || viLinks.primary() == null) return LinksImpl.EMPTY;
            Links links = viLinks.removeIfFromTo(v -> !LinkVariable.acceptForLinkedVariables(v));
            if (ignoreReturnValue.contains(pi)) {
                return links.removeIfTo(v -> v instanceof ReturnVariable);
            }
            return links;
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

            Result r;
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
                            .reduce(ExpressionVisitor.EMPTY, Result::merge);
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
                    Result u = expressionVisitor.visit(e, previousVd, stageOfPrevious);
                    r = r == null ? u : r.merge(u);
                }
            }
            if (forEachLv != null) {
                Expression e = javaInspector.runtime().sortAndSimplify(true, statement.expression());
                Result u = new ForEach(javaInspector.runtime(), expressionVisitor)
                        .linkIntoIterable(forEachLv.parameterizedType(), e, previousVd, stageOfPrevious);
                Links newLinks = new LinksImpl.Builder(forEachLv)
                        .add(LinkNatureImpl.IS_ASSIGNED_FROM, u.links().primary())
                        .build();
                r = u.addExtra(Map.of(forEachLv, newLinks));
            }
            if (statement instanceof ForStatement fs) {
                for (Expression updater : fs.updaters()) {
                    Expression sorted = javaInspector.runtime().sortAndSimplify(true, updater);
                    Result u = expressionVisitor.visit(sorted, previousVd, stageOfPrevious);
                    r = r == null ? u : r.merge(u);
                }
            }
            TranslationMap replaceConstants;
            if (r != null) {
                this.variablesRepresentingConstants.addAll(r.variablesRepresentingConstants());
                replaceConstants = replaceConstants(previousVd, stageOfPrevious);
            } else {
                replaceConstants = null;
            }
            VariableData vd = VariableDataImpl.of(statement);
            Variable destination;
            if (r != null) {
                r = r.copyLinksToExtra();
            }
            if (statement instanceof ReturnStatement && methodInfo.hasReturnValue()) {
                if (r != null && r.links().primary() != null) {
                    destination = r.links().primary();
                } else {
                    destination = MarkerVariable.someValue(javaInspector.runtime(), methodInfo.returnType());
                }
                Links rvLinks = new LinksImpl.Builder(returnVariable)
                        .add(LinkNatureImpl.IS_ASSIGNED_FROM, destination)
                        .build();
                if (r != null) {
                    r = r.with(rvLinks);
                    r = r.copyLinksToExtra();
                } else {
                    r = new Result(rvLinks, LinkedVariablesImpl.EMPTY);
                }
            }
            Map<LinkGraph.V, Map<LinkGraph.V, LinkNature>> graph;
            if (r != null) {
                this.erase.addAll(r.erase());
                graph = linkGraph.compute(r.extra().map(), previousVd, stageOfPrevious, vd, replaceConstants,
                        r.modified());
            } else {
                graph = Map.of();
            }
            Set<Variable> previouslyModified = computePreviouslyModified(vd, previousVd, stageOfPrevious);
            WriteLinksAndModification.WriteResult wr = writeLinksAndModification.go(statement, vd, previouslyModified,
                    r == null ? Map.of() : r.modified(), graph);
            copyEvalIntoVariableData(wr.newLinks(), vd);
            modificationsOutsideVariableData.addAll(wr.modifiedOutsideVariableData());


            writeCasts(r == null ? new HashMap<>() : r.casts(), previousVd, stageOfPrevious, vd);

            if (statement.hasSubBlocks()) {
                handleSubBlocks(statement, vd);
            }
            if (r != null) {
                writeOutMethodCallAnalysis(r.writeMethodCalls(), vd);
            }
            return vd;
        }

        private Set<Variable> computePreviouslyModified(VariableData vd, VariableData previousVd, Stage stageOfPrevious) {
            if (previousVd != null) {
                return previousVd.variableInfoStream(stageOfPrevious)
                        .filter(vi -> !(vi.variable() instanceof This))
                        .filter(vi -> vd.isKnown(vi.variable().fullyQualifiedName()))
                        .map(vi -> {
                            Value.Bool unmodified = vi.analysis().getOrNull(UNMODIFIED_VARIABLE, ValueImpl.BoolImpl.class);
                            boolean explicitlyModified = unmodified != null && unmodified.isFalse();
                            return explicitlyModified ? vi.variable() : null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet());
            }
            return Set.of();
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
                    for (Variable v : Util.goUp(link.from())) {
                        if (vd.isKnown(v.fullyQualifiedName())) {
                            variablesLinkedToObject.put(v, true);
                        }
                    }
                    for (Variable v : Util.goUp(link.to())) {
                        if (vd.isKnown(v.fullyQualifiedName())) {
                            variablesLinkedToObject.put(v, false);
                        }
                    }

                }
            }
        }

        private void handleSubBlocks(Statement statement, VariableData vd) {
            List<VariableData> vds = statement.subBlockStream()
                    .filter(block -> !block.isEmpty())
                    .map(block -> doBlock(block, vd))
                    .filter(Objects::nonNull)
                    .toList();
            vd.variableInfoContainerStream()
                    .filter(VariableInfoContainer::hasMerge)
                    .forEach(vic -> {
                        VariableInfo viEval = vic.best(Stage.EVALUATION);
                        Variable variable = viEval.variable();
                        Links eval = viEval.linkedVariables();
                        String fqn = variable.fullyQualifiedName();
                        Links.Builder collect = eval == null || eval.primary() == null
                                ? new LinksImpl.Builder(variable)
                                : new LinksImpl.Builder(eval);
                        AtomicBoolean unmodified = new AtomicBoolean(true);
                        Set<TypeInfo> downcasts = new HashSet<>(viEval.analysis().getOrDefault(DOWNCAST_VARIABLE,
                                ValueImpl.SetOfTypeInfoImpl.EMPTY).typeInfoSet());
                        vds.forEach(subVd -> {
                            VariableInfoContainer subVic = subVd.variableInfoContainerOrNull(fqn);
                            if (subVic != null) {
                                VariableInfo subVi = subVic.best();
                                Links subTlv = subVi.linkedVariables();
                                if (subTlv != null && subTlv.primary() != null) {
                                    collect.addAllDistinct(subTlv);
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

        private void copyEvalIntoVariableData(Map<Variable, Links> expanded, VariableData vd) {
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

        public Result handleLvc(LocalVariableCreation lvc,
                                VariableData previousVd,
                                Stage stageOfPrevious) {
            return lvc.localVariableStream()
                    .map(lv -> handleSingleLvc(previousVd, stageOfPrevious, lv))
                    .reduce(ExpressionVisitor.EMPTY, Result::merge);
        }

        private @NotNull Result handleSingleLvc(VariableData previousVd,
                                                Stage stageOfPrevious,
                                                LocalVariable lv) {
            Result r;
            if (!lv.assignmentExpression().isEmpty()) {
                Map<Variable, Links> linkedVariables = new HashMap<>();
                Expression e = javaInspector.runtime().sortAndSimplify(true, lv.assignmentExpression());
                r = expressionVisitor.visit(e, previousVd, stageOfPrevious);
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
