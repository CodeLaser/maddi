package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.defaults.ShallowMethodAnalyzer;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.NullConstant;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.ExpressionVisitor.EMPTY;
import static org.e2immu.analyzer.modification.link.impl.LinkGraph.followGraph;
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
    private static final TimedLogger TIMED = new TimedLogger(LOGGER, 1000L);

    public static final PropertyImpl VARIABLES_LINKED_TO_OBJECT = new PropertyImpl("variablesLinkedToObject",
            ValueImpl.VariableBooleanMapImpl.EMPTY);
    public static final PropertyImpl LINKED_VARIABLES_ARGUMENTS = new PropertyImpl("linkedVariablesArguments",
            ListOfLinksImpl.EMPTY);

    public record ListOfLinksImpl(List<Links> list) implements ListOfLinks {
        public static final ListOfLinks EMPTY = new ListOfLinksImpl(List.of());

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            return null;
        }

        @Override
        public boolean isDefault() {
            return list.isEmpty();
        }

        @Override
        public boolean overwriteAllowed(Value newValue) {
            // TODO
            return true;
        }
    }

    private final Options options;
    private final JavaInspector javaInspector;
    private final RecursionPrevention recursionPrevention;
    private final ShallowMethodLinkComputer shallowMethodLinkComputer;
    private final LinkGraph linkGraph;
    private final WriteLinksAndModification writeLinksAndModification;
    private final ShallowMethodAnalyzer shallowMethodAnalyzer;
    private final AtomicInteger propertiesChanged;
    private final AtomicInteger variableCounter = new AtomicInteger();
    private final AtomicInteger countSourceMethods = new AtomicInteger();
    private final VirtualFieldComputer virtualFieldComputer;

    // for testing
    public LinkComputerImpl(JavaInspector javaInspector) {
        this(javaInspector, Options.TEST, new AtomicInteger());
    }

    // for testing
    public LinkComputerImpl(JavaInspector javaInspector, Options options) {
        this(javaInspector, options, new AtomicInteger());
    }

    public LinkComputerImpl(JavaInspector javaInspector, Options options, AtomicInteger propertiesChanged) {
        this.recursionPrevention = new RecursionPrevention(options.recurse());
        this.javaInspector = javaInspector;
        this.options = options;
        this.virtualFieldComputer = new VirtualFieldComputer(javaInspector);
        this.shallowMethodLinkComputer = new ShallowMethodLinkComputer(javaInspector.runtime(), virtualFieldComputer);
        this.linkGraph = new LinkGraph(javaInspector, javaInspector.runtime(), options.checkDuplicateNames());
        this.writeLinksAndModification = new WriteLinksAndModification(javaInspector, javaInspector.runtime(), virtualFieldComputer);
        this.shallowMethodAnalyzer = new ShallowMethodAnalyzer(javaInspector.runtime(), Element::annotations);
        this.propertiesChanged = propertiesChanged;
    }

    @Override
    public int propertiesChanged() {
        return propertiesChanged.get();
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
    public void doAnonymousType(TypeInfo typeInfo) {
        doType(typeInfo);
    }

    @Override
    public MethodLinkedVariables doMethod(MethodInfo method) {
        try {
            TypeInfo typeInfo = method.typeInfo();
            boolean shallow = options.forceShallow() || method.isAbstract() || typeInfo.compilationUnit().externalLibrary();
            return doMethod(method, shallow, false, true);
        } catch (RuntimeException | AssertionError e) {
            LOGGER.error("Caught exception computing {}", method, e);
            throw e;
        }
    }

    /*
    NOTE: While it is possible (and part of the overall analyzer's strategy) to re-run the LinkComputer on
    certain methods, recurseMethod() never forces a re-run. You should explicitly call doMethod().
     */
    @Override
    public MethodLinkedVariables recurseMethod(MethodInfo method) {
        MethodLinkedVariables alreadyDone = method.analysis().getOrNull(METHOD_LINKS, MethodLinkedVariablesImpl.class);
        if (alreadyDone != null) return alreadyDone;
        try {
            TypeInfo typeInfo = method.typeInfo();
            boolean shallow = options.forceShallow() || typeInfo.compilationUnit().externalLibrary();
            return doMethod(method, shallow, true, true);
        } catch (RuntimeException | AssertionError e) {
            LOGGER.error("Caught exception recursively computing {}", method, e);
            throw e;
        }
    }

    @Override
    public MethodLinkedVariables doMethodShallowDoNotWrite(MethodInfo methodInfo) {
        return doMethod(methodInfo, true, false, false);
    }

    private MethodLinkedVariables doMethod(MethodInfo methodInfo, boolean shallow, boolean write, boolean writeShallow) {
        if (shallow) {
            // FIXME what if we want to use annotations to help when !write? then they will not be seen
            if (writeShallow && !methodInfo.analysis().haveAnalyzedValueFor(PropertyImpl.DEFAULTS_ANALYZER)) {
                shallowMethodAnalyzer.analyze(methodInfo);
            }
            MethodLinkedVariables mlv = shallowMethodLinkComputer.go(methodInfo);
            if (write) {
                if (methodInfo.analysis().setAllowControlledOverwrite(METHOD_LINKS, mlv)) {
                    propertiesChanged.incrementAndGet();
                }
            }
            return mlv;
        }
        MethodLinkedVariables tlv;
        if (recursionPrevention.sourceAllowed(methodInfo)) {
            try {
                tlv = new SourceMethodComputer(methodInfo).go();
                if (write) {
                    if (methodInfo.analysis().setAllowControlledOverwrite(METHOD_LINKS, tlv)) {
                        propertiesChanged.incrementAndGet();
                    }
                }
            } finally {
                recursionPrevention.doneSource(methodInfo);
            }
        } else {
            // we're already analyzing methodInfo... so we return a shallow copy, not written out!
            LOGGER.debug("Fall-back to shallow for {}", methodInfo);
            tlv = doMethod(methodInfo, true, false, false);
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
        final Stack<Links.Builder> yieldStack = new Stack<>();

        public SourceMethodComputer(MethodInfo methodInfo) {
            this.methodInfo = methodInfo;
            this.expressionVisitor = new ExpressionVisitor(javaInspector.runtime(),
                    javaInspector, options, new VirtualFieldComputer(javaInspector),
                    LinkComputerImpl.this, this, methodInfo, recursionPrevention,
                    variableCounter);
            this.returnVariable = methodInfo.hasReturnValue() ? new ReturnVariableImpl(methodInfo) : null;
        }

        // both methods called from ExpressionVisitor.evaluateSwitchEntry
        void startSwitchExpression(Variable primary) {
            yieldStack.push(new LinksImpl.Builder(primary));
        }

        Links endSwitchExpression() {
            return yieldStack.pop().build();
        }

        private TranslationMap replaceConstants(VariableData vd, Stage stage) {
            TranslateConstants tc = new TranslateConstants(javaInspector.runtime());
            for (LocalVariable lv : variablesRepresentingConstants) {
                if (!(lv.assignmentExpression() instanceof NullConstant)) {
                    assert lv.parameterizedType().arrays() == 0;
                    tc.put(lv, lv.assignmentExpression());
                }
            }
            if (vd != null) {
                for (VariableInfo vi : vd.variableInfoIterable(stage)) {
                    if (vi.linkedVariables() != null) {
                        for (Link l : vi.linkedVariables()) {
                            if (l.from().equals(vi.variable())
                                && l.linkNature().isIdenticalToOrAssignedFromTo()
                                && l.to() instanceof LocalVariable) {
                                Expression constant = tc.get(l.to());
                                if (constant != null && !(constant instanceof NullConstant)) {
                                    assert vi.variable().parameterizedType().arrays() == 0;
                                    tc.put(vi.variable(), constant);
                                }
                            }
                        }
                    }
                }
            }
            return tc;
        }

        // TODO: copy linked variables of a variable in closure to others in the closure, to that closure
        public MethodLinkedVariables go() {
            VariableData vd = doBlock(true, methodInfo.methodBody(), null);
            Links ofReturnValue = vd == null || returnVariable == null
                                  || !vd.isKnown(returnVariable.fullyQualifiedName())
                    ? LinksImpl.EMPTY
                    : emptyIfOnlySomeValue(vd.variableInfo(returnVariable).linkedVariables());
            Set<ParameterInfo> paramsInOfReturnValue = ofReturnValue.stream()
                    .flatMap(Link::parameterStream)
                    .collect(Collectors.toUnmodifiableSet());
            List<Links> ofParameters = methodInfo.parameters().stream()
                    .map(pi -> filteredPi(pi, paramsInOfReturnValue, vd)).toList();
            Set<Variable> inClosure = vd == null ? Set.of()
                    : vd.variableInfoStream()
                    .filter(VariableInfo::isVariableInClosure)
                    .map(VariableInfo::variable).collect(Collectors.toUnmodifiableSet());
            Set<Variable> modified = vd == null ? Set.of()
                    : vd.variableInfoStream()
                    .filter(vi -> !vi.variable().equals(returnVariable)
                                  && (vi.isVariableInClosure()
                                      || LinkVariable.acceptForLinkedVariables(vi.variable())))
                    .filter(VariableInfo::isModified)
                    .map(VariableInfo::variable)
                    .collect(Collectors.toUnmodifiableSet());
            Set<Variable> modifiedOutside = this.modificationsOutsideVariableData.stream()
                    .filter(v -> inClosure.contains(v) || LinkVariable.acceptForLinkedVariables(v))
                    .collect(Collectors.toUnmodifiableSet());
            Set<Variable> allModified = Stream.concat(modified.stream(), modifiedOutside.stream())
                    .collect(Collectors.toUnmodifiableSet());
            MethodLinkedVariables mlv = new MethodLinkedVariablesImpl(ofReturnValue, ofParameters, allModified);
            copyModificationsIntoMethod(allModified, inClosure, mlv);
            if (vd != null) copyDowncastIntoParameters(vd);

            countSourceMethods.incrementAndGet();
            return mlv;
        }

        private void copyDowncastIntoParameters(VariableData vd) {
            Map<ParameterInfo, Map<Variable, Set<TypeInfo>>> all = new HashMap<>();
            for (VariableInfo vi : vd.variableInfoIterable()) {
                ParameterInfo pi = Util.parameterPrimaryOrNull(vi.variable());
                if (pi != null) {
                    Value.SetOfTypeInfo fromVariable = vi.analysis().getOrDefault(DOWNCAST_VARIABLE,
                            ValueImpl.SetOfTypeInfoImpl.EMPTY);
                    if (!fromVariable.typeInfoSet().isEmpty()) {
                        Map<Variable, Set<TypeInfo>> map = all.computeIfAbsent(pi, _ -> new HashMap<>());
                        map.merge(vi.variable(), fromVariable.typeInfoSet(),
                                (s0, s1) ->
                                        Stream.concat(s0.stream(), s1.stream()).collect(Collectors.toUnmodifiableSet()));
                    }
                }
            }
            for (Map.Entry<ParameterInfo, Map<Variable, Set<TypeInfo>>> entry : all.entrySet()) {
                ParameterInfo pi = entry.getKey();
                var v2tiSet = new ValueImpl.VariableToTypeInfoSetImpl(entry.getValue());
                if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.DOWNCAST_PARAMETER, v2tiSet)) {
                    propertiesChanged.incrementAndGet();
                }
            }
        }

        private void copyModificationsIntoMethod(Set<Variable> modified, Set<Variable> inClosure, MethodLinkedVariables mlv) {
            boolean methodModified = false;
            boolean[] paramsModified = new boolean[methodInfo.parameters().size()];
            for (Variable v : modified) {
                if (v instanceof This thisVar && thisVar.typeInfo().equals(methodInfo.typeInfo())
                    || v instanceof FieldReference fr && fr.scopeIsRecursivelyThis()
                    || inClosure.contains(v)) {
                    methodModified = true;
                } else if (v instanceof ParameterInfo pi && pi.methodInfo().equals(methodInfo)) {
                    paramsModified[pi.index()] = true;
                }
            }
            Value.Bool nonModifying = ValueImpl.BoolImpl.from(!methodModified);
            if (methodInfo.analysis().setAllowControlledOverwrite(PropertyImpl.NON_MODIFYING_METHOD, nonModifying)) {
                propertiesChanged.incrementAndGet();
            }
            for (ParameterInfo pi : methodInfo.parameters()) {
                Links links = mlv.ofParameters().get(pi.index());
                if (links.stream().noneMatch(l -> l.to().variableStreamDescend()
                        .anyMatch(v -> v instanceof FieldReference fr && inCurrentHierarchy(fr.fieldInfo().owner())))) {
                    Value.Bool unmodified = ValueImpl.BoolImpl.from(!paramsModified[pi.index()]);
                    if (pi.analysis().setAllowControlledOverwrite(PropertyImpl.UNMODIFIED_PARAMETER, unmodified)) {
                        propertiesChanged.incrementAndGet();
                    }
                } // else: we'll need to wait until we know about all the links of the field; see TestFieldAnalyzer
            }
        }

        private boolean inCurrentHierarchy(TypeInfo typeInfo) {
            return typeInfo.equals(methodInfo.typeInfo())
                   || methodInfo.typeInfo().superTypesExcludingJavaLangObject().contains(typeInfo);
        }

        private Links emptyIfOnlySomeValue(Links links) {
            if (links == null) return LinksImpl.EMPTY;
            if (links.stream().allMatch(l ->
                    l.to() instanceof MarkerVariable mv && (mv.isSomeValue() || mv.isConstant()))) {
                return LinksImpl.EMPTY;
            }
            if (!links.isEmpty()) {
                // remove links to lambda parameters, which we keep for VL2O in VariableInfo.linkedVariables
                Links links2 = links.removeIfTo(this::isParameterOfStrictlyEnclosed);
                // remove SomeValue unless p0 is present
                ParameterInfo p0 = methodInfo.parameters().isEmpty() ? null : methodInfo.parameters().getFirst();
                if (p0 == null || links2.stream().noneMatch(l ->
                        l.from().equals(links2.primary()) &&
                        l.linkNature().isIdenticalToOrAssignedFromTo() &&
                        l.to() instanceof ParameterInfo pi && pi.equals(p0))) {
                    return links2.removeIfTo(v -> v instanceof MarkerVariable mv && mv.isSomeValue());
                }
                return links2;
            }
            return links;
        }

        private Links filteredPi(ParameterInfo pi, Set<ParameterInfo> ignoreReturnValue, VariableData vd) {
            if (vd == null) return LinksImpl.EMPTY;
            VariableInfoContainer vic = vd.variableInfoContainerOrNull(pi.fullyQualifiedName());
            if (vic == null) return LinksImpl.EMPTY;
            VariableInfo vi = vic.best();
            Links viLinks = vi.linkedVariables();
            if (viLinks == null || viLinks.primary() == null) return LinksImpl.EMPTY;
            Links links = viLinks.removeIfFromTo(v -> !LinkVariable.acceptForLinkedVariables(v)
                                                      || isParameterOfSiblingMethod(v));
            if (ignoreReturnValue.contains(pi)) {
                return links.removeIfTo(v -> v instanceof ReturnVariable || isParameterOfStrictlyEnclosed(v));
            }
            return links.removeIfTo(this::isParameterOfStrictlyEnclosed);
        }

        // see TestForEachLambda,5 and TestForEachMethodReference
        private boolean isParameterOfSiblingMethod(Variable v) {
            return v instanceof ParameterInfo otherParameter
                   && otherParameter.methodInfo() != methodInfo
                   && otherParameter.methodInfo().typeInfo() == methodInfo.typeInfo();
        }

        private boolean isParameterOfStrictlyEnclosed(Variable variable) {
            ParameterInfo pi = Util.parameterPrimaryOrNull(variable);
            return pi != null && pi.methodInfo().typeInfo().isStrictlyEnclosedIn(methodInfo.typeInfo());
        }

        VariableData doBlock(boolean topBlock, Block block, VariableData previousVd) {
            VariableData vd = previousVd;
            boolean firstStatementOfBlock = true;
            for (Statement statement : block.statements()) {
                if (statement instanceof Block b) {
                    // a block among the statements
                    vd = doBlock(false, b, vd);
                } else {
                    try {
                        boolean lastStatement = topBlock && statement == block.statements().getLast();
                        vd = doStatement(statement, lastStatement, vd, firstStatementOfBlock);
                    } catch (RuntimeException | AssertionError re) {
                        LOGGER.error("Caught exception in statement {} of {}: {}", statement.source(), methodInfo,
                                re.getMessage());
                        throw re;
                    }
                }
                firstStatementOfBlock = false;
            }
            return vd;
        }

        public VariableData doStatement(Statement statement,
                                        boolean lastStatement,
                                        VariableData previousVd,
                                        boolean firstStatementOfBlock) {
            Stage stageOfPrevious = firstStatementOfBlock ? Stage.EVALUATION : Stage.MERGE;
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
                            .reduce(EMPTY, Result::merge);
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
                if (u.links().primary() != null) {
                    Links newLinks = new LinksImpl.Builder(forEachLv)
                            .add(LinkNatureImpl.IS_ASSIGNED_FROM, u.links().primary())
                            .build();
                    r = u.addExtra(Map.of(forEachLv, newLinks));
                } // else: most often: a recursive method, which has an empty result
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
            } else if (statement instanceof YieldStatement && r != null && r.links().primary() != null) {
                Links.Builder current = yieldStack.peek();
                current.add(LinkNatureImpl.IS_ASSIGNED_FROM, r.links().primary());
            }
            Map<Variable, Map<Variable, LinkNature>> graph;
            if (r != null) {
                this.erase.addAll(r.erase());
                graph = linkGraph.compute(r.extra().map(), previousVd, stageOfPrevious, vd, replaceConstants,
                        r.modified());
            } else {
                graph = Map.of();
            }
            Set<Variable> previouslyModified = computePreviouslyModified(vd, previousVd, stageOfPrevious);
            WriteLinksAndModification.WriteResult wr = writeLinksAndModification.go(statement, lastStatement, vd, previouslyModified,
                    r == null ? Map.of() : r.modified(), graph);
            copyEvalIntoVariableData(wr.newLinks(), vd);
            modificationsOutsideVariableData.addAll(wr.modifiedOutsideVariableData());

            TIMED.info("Done {} methods; do statement {} {} graph size {}, sum of links {}",
                    countSourceMethods.get(),
                    methodInfo.fullyQualifiedName(),
                    statement.source().index(), graph.size(), wr.newLinksSize());
           /* if (wr.newLinksSize() > 1000) {
                double fraction = (double) wr.newLinksSize() / (graph.size() * graph.size());
                if (fraction > 0.4) {
                    LOGGER.error("Do statement {} {} graph size {}, sum of links {}\n\n", methodInfo.fullyQualifiedName(),
                            statement.source().index(), graph.size(), wr.newLinksSize());
                    for (Map.Entry<Variable, Links> entry : wr.newLinks().entrySet()) {
                        LOGGER.error("{} -> {}", entry.getKey(), entry.getValue());
                    }
                    throw new UnsupportedOperationException();
                }
            }*/
            writeCasts(r == null ? new HashMap<>() : r.casts(), previousVd, stageOfPrevious, vd);

            if (statement.hasSubBlocks()) {
                handleSubBlocks(statement, vd);
            }
            if (r != null) {
                writeOutMethodCallAnalysis(r.writeMethodCalls(), vd, graph);
            }
            return vd;
        }

        private Set<Variable> computePreviouslyModified(VariableData vd, VariableData previousVd, Stage stageOfPrevious) {
            if (previousVd != null) {
                return previousVd.variableInfoStream(stageOfPrevious)
                        .filter(vi -> vd.isKnown(vi.variable().fullyQualifiedName()))
                        .filter(VariableInfo::isModified)
                        .map(VariableInfo::variable)
                        .collect(Collectors.toUnmodifiableSet());
            }
            return Set.of();
        }

        private void writeCasts(Map<Variable, Set<TypeInfo>> castsIn,
                                VariableData previous,
                                Stage stageOfPrevious,
                                VariableData variableData) {
            Map<Variable, Set<TypeInfo>> casts;
            // we may otherwise be running computeIfAbsent on Map.of() in EMPTY
            if (previous != null) {
                casts = new HashMap<>(castsIn);
                previous.variableInfoStream(stageOfPrevious).forEach(vi -> {
                    if (variableData.isKnown(vi.variable().fullyQualifiedName())) {
                        Value.SetOfTypeInfo set = vi.analysis().getOrDefault(VariableInfoImpl.DOWNCAST_VARIABLE,
                                ValueImpl.SetOfTypeInfoImpl.EMPTY);
                        if (!set.typeInfoSet().isEmpty()) {
                            casts.computeIfAbsent(vi.variable(), _ -> new HashSet<>()).addAll(set.typeInfoSet());
                        }
                    }
                });
            } else {
                casts = castsIn;
            }
            casts.forEach((v, set) -> {
                VariableInfoContainer vic = variableData.variableInfoContainerOrNull(v.fullyQualifiedName());
                if (vic != null) {
                    VariableInfoImpl vii = (VariableInfoImpl) vic.best(Stage.EVALUATION);
                    if (vii.analysis().setAllowControlledOverwrite(VariableInfoImpl.DOWNCAST_VARIABLE,
                            new ValueImpl.SetOfTypeInfoImpl(Set.copyOf(set)))) {
                        propertiesChanged.incrementAndGet();
                    }
                }
            });
        }

        private void writeOutMethodCallAnalysis(List<ExpressionVisitor.WriteMethodCall> writeMethodCalls,
                                                VariableData vd,
                                                Map<Variable, Map<Variable, LinkNature>> graph) {
            for (ExpressionVisitor.WriteMethodCall wmc : writeMethodCalls) {
                // rather than taking the links in wmc, we want the fully expanded links
                // (after running copyEvalIntoVariableData)
                Map<Variable, Boolean> variablesLinkedToObject = new HashMap<>();
                for (Link l : wmc.linksFromObject()) {
                    addToVariablesLinkedToObject(vd, graph, l.to(), variablesLinkedToObject);
                }
                addToVariablesLinkedToObject(vd, graph, wmc.linksFromObject().primary(), variablesLinkedToObject);
                if (!variablesLinkedToObject.isEmpty()
                    // only write once, no point because actual variables in links will not change
                    && !wmc.methodCall().analysis().haveAnalyzedValueFor(VARIABLES_LINKED_TO_OBJECT)) {
                    try {
                        wmc.methodCall().analysis().set(VARIABLES_LINKED_TO_OBJECT,
                                new ValueImpl.VariableBooleanMapImpl(Map.copyOf(variablesLinkedToObject)));
                        propertiesChanged.incrementAndGet();
                    } catch (IllegalArgumentException iae) {
                        LinkComputerImpl.this.recursionPrevention.report(methodInfo);
                        throw iae;
                    }
                }
            }
        }

        private void addToVariablesLinkedToObject(VariableData vd,
                                                  Map<Variable, Map<Variable, LinkNature>> graph,
                                                  Variable sub,
                                                  Map<Variable, Boolean> variablesLinkedToObject) {
            Variable primary = Util.primary(sub);
            if (primary != null && vd.isKnown(primary.fullyQualifiedName())) {
                variablesLinkedToObject.put(primary, true);
                VariableInfo viPrimary = vd.variableInfo(primary);
                Links links = followGraph(virtualFieldComputer, graph, viPrimary.variable()).build();
                for (Link link : links) {
                    if (!link.linkNature().isIdenticalTo()) {
                        for (Variable v : Util.goUp(link.from())) {
                            if (acceptForVL2O(v)) {
                                variablesLinkedToObject.put(v, true);
                            }
                        }
                        for (Variable v : Util.goUp(link.to())) {
                            if (acceptForVL2O(v)) {
                                variablesLinkedToObject.put(v, false);
                            }
                        }
                    }
                }
            }
        }

        private static boolean acceptForVL2O(Variable v) {
            return !Util.virtual(v) && !(v instanceof MarkerVariable) && !(v instanceof IntermediateVariable);
        }

        private void handleSubBlocks(Statement statement, VariableData vd) {
            List<VariableData> vds = statement.subBlockStream()
                    .filter(block -> !block.isEmpty())
                    .map(block -> doBlock(false, block, vd))
                    .filter(Objects::nonNull)
                    .toList();
            handleSubBlocks(vds, vd);
        }

        // also called from ExpressionVisitor.switchExpression
        void handleSubBlocks(List<VariableData> vds, VariableData vd) {
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
                        AtomicBoolean unmodified = new AtomicBoolean(viEval.isUnmodified());
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
                                if (subVi.isModified()) unmodified.set(false);

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
                        if (merge.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE,
                                ValueImpl.BoolImpl.from(unmodified.get()))) {
                            propertiesChanged.incrementAndGet();
                        }
                        if (!downcasts.isEmpty()) {
                            if (merge.analysis().setAllowControlledOverwrite(DOWNCAST_VARIABLE,
                                    new ValueImpl.SetOfTypeInfoImpl(Set.copyOf(downcasts)))) {
                                propertiesChanged.incrementAndGet();
                            }
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
                    .reduce(EMPTY, Result::merge);
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
            return EMPTY;
        }
    }
}
