package org.e2immu.analyzer.modification.prepwork;

import org.e2immu.analyzer.modification.common.getset.ApplyGetSetTranslation;
import org.e2immu.analyzer.modification.prepwork.escape.ComputeAlwaysEscapes;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.*;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.support.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.prepwork.StatementIndex.*;

/*
do all the analysis of this phase

 */
public class MethodAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodAnalyzer.class);

    /*
    we cannot store the VariableInfo object, because it is not known at the moment of construction (which is
    before the eval VIs are created)
     */
    public record VariableInfoMap(Set<String> variableNames, VariableData variableData) implements Value {
        public boolean contains(String fqn) {
            return variableNames.contains(fqn);
        }

        @Override
        public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDefault() {
            return variableData.isDefault() && variableNames.isEmpty();
        }

        public VariableData get(String fqn) {
            return variableNames.contains(fqn) ? variableData : null;
        }

        public boolean isEmpty() {
            return variableNames.isEmpty();
        }

        public String sortedByFqn() {
            return variableNames.stream().sorted().collect(Collectors.joining(", "));
        }
    }

    public static final VariableInfoMap EMPTY_VARIABLE_INFO_MAP = new VariableInfoMap(Set.of(), null);
    public static final Property VARIABLES_OF_ENCLOSING_METHOD = new PropertyImpl("localVariablesOfEnclosingMethod",
            EMPTY_VARIABLE_INFO_MAP);

    private final Runtime runtime;
    private final PrepAnalyzer prepAnalyzer;
    private final ApplyGetSetTranslation applyGetSetTranslation;

    public MethodAnalyzer(Runtime runtime, PrepAnalyzer prepAnalyzer) {
        this.runtime = runtime;
        this.prepAnalyzer = prepAnalyzer;
        this.applyGetSetTranslation = new ApplyGetSetTranslation(runtime);
    }

    private static class InternalVariables {
        final InternalVariables parent;

        // variables that are copied
        final ReturnVariable rv;
        final Stack<Statement> loopSwitchStack;
        final Map<String, String> labelToStatementIndex;
        final Map<String, Integer> breakCountsInLoop;
        final VariableInfoMap closure;

        // variables that are searched via parent
        LocalVariable breakVariable;
        final Map<String, String> limitedScopeOfPatternVariables = new HashMap<>();

        InternalVariables() {
            this(null, EMPTY_VARIABLE_INFO_MAP);
        }

        InternalVariables(ReturnVariable rv, VariableInfoMap closure) {
            this.rv = rv;
            this.parent = null;
            loopSwitchStack = new Stack<>();
            labelToStatementIndex = new HashMap<>();
            breakCountsInLoop = new HashMap<>();
            this.closure = closure;
        }

        InternalVariables(InternalVariables parent) {
            this.parent = parent;
            this.rv = parent.rv;
            this.loopSwitchStack = parent.loopSwitchStack;
            this.labelToStatementIndex = parent.labelToStatementIndex;
            this.breakCountsInLoop = parent.breakCountsInLoop;
            this.closure = parent.closure;
        }

        public boolean acceptLimitedScope(VariableData variableData, Variable variable, String indexOfDefinition, String index) {
            String i = limitedScopeOfPatternVariablesGet(variableData, variable, indexOfDefinition);
            if (i != null) {
                return Util.inScopeOf(i, index);
            }
            // go up, maybe it was defined higher up
            if (parent != null) {
                return parent.acceptLimitedScope(variableData, variable, indexOfDefinition, index);
            }
            return true;
        }

        List<Variable> currentVariables() {
            if (breakVariable != null) return List.of(rv, breakVariable);
            if (parent == null) return List.of(rv);
            return parent.currentVariables();
        }

        void handleStatement(String index, Statement statement) {
            if (statement.label() != null) {
                labelToStatementIndex.put(statement.label(), index);
            }
            if (statement instanceof SwitchStatementOldStyle || statement instanceof LoopStatement) {
                loopSwitchStack.push(statement);
            } else if (statement instanceof BreakStatement bs) {
                String loopIndex;
                if (bs.label() == null) {
                    loopIndex = loopSwitchStack.peek().source().index();
                } else {
                    loopIndex = labelToStatementIndex.get(bs.label());
                }
                breakCountsInLoop.merge(loopIndex, 1, Integer::sum);
            }
        }

        void endHandleStatement(Statement statement) {
            if (statement instanceof SwitchStatementOldStyle || statement instanceof LoopStatement) {
                loopSwitchStack.pop();
            }
        }

        public String limitedScopeOfPatternVariablesGet(VariableData variableData, Variable v, String indexOfDefinition) {
            String scope = limitedScopeOfPatternVariables.get(indexOfDefinition + "-" + v.simpleName());
            if (scope != null) return scope;
            if (v instanceof FieldReference fr && fr.scopeVariable() != null) {
                String indexOfDefinitionScope = variableData.indexOfDefinitionOrNull(fr.scopeVariable());
                return indexOfDefinitionScope == null ? null
                        : limitedScopeOfPatternVariablesGet(variableData, fr.scopeVariable(), indexOfDefinitionScope);
            }
            if (v instanceof DependentVariable dv) {
                String indexOfDefinitionAv = dv.arrayVariable() == null ? null
                        : variableData.indexOfDefinitionOrNull(dv.arrayVariable());
                String s1 = indexOfDefinitionAv == null ? null
                        : limitedScopeOfPatternVariablesGet(variableData, dv.arrayVariable(), indexOfDefinitionAv);
                String indexOfDefinitionIv = dv.indexVariable() == null ? null
                        : variableData.indexOfDefinitionOrNull(dv.indexVariable());
                String s2 = dv.indexVariable() == null ? null
                        : limitedScopeOfPatternVariablesGet(variableData, dv.indexVariable(), indexOfDefinitionIv);
                if (s2 == null) return s1;
                if (s1 == null) return s2;
                String t1 = Util.stripStage(s1);
                String t2 = Util.stripStage(s2);
                if (t1.contains(t2)) return t2;// the most restrictive
                if (t2.contains(t1)) return t1;
                throw new UnsupportedOperationException("disjoint, is this possible?");
            }
            return null;
        }

        public void limitedScopeOfPatternVariablesPut(Variable v, String indexOfDefinition, String limitedScope) {
            limitedScopeOfPatternVariables.put(indexOfDefinition + "-" + v.simpleName(), limitedScope);
        }

        void setBreakVariable(LocalVariable bv) {
            breakVariable = bv;
        }

        LocalVariable bv() {
            if (breakVariable != null) return breakVariable;
            if (parent == null) return null;
            return parent.bv();
        }
    }

    private record ReadWriteData(VariableData previous,
                                 Map<Variable, String> seenFirstTime,
                                 Map<Variable, String> accessorSeenFirstTime,
                                 Map<Variable, List<String>> read,
                                 Map<Variable, List<String>> assigned,
                                 Map<Variable, String> restrictToScope) {
        public Assignments assignmentIds(Variable v, VariableInfo previous) {
            Assignments prev;
            if (previous == null) {
                assert !(v instanceof LocalVariable);
                prev = new Assignments(BEFORE_METHOD);
            } else {
                prev = previous.assignments();
            }
            List<String> assignIds = assigned.get(v);
            if (assignIds != null) {
                return Assignments.newAssignment(assignIds, prev);
            }
            return prev;
        }

        public Reads isRead(Variable v, VariableInfo previous) {
            List<String> i = read.get(v);
            if (i == null) {
                return previous == null ? Reads.NOT_YET_READ : previous.reads();
            }
            return previous == null ? new Reads(i) : previous.reads().with(i);
        }
    }

    public void doMethod(MethodInfo methodInfo) {
        doMethod(methodInfo, methodInfo.methodBody());
    }

    public void doMethod(MethodInfo methodInfo, Block methodBody) {
        LOGGER.debug("Prep: do method {} @line {}", methodInfo,
                methodInfo.source() == null ? "?" : methodInfo.source().compact2());
        // even if the method does not return a value, we'll compute "assignments" to the return variable,
        // in order to know when the method exits (we'll track 'throw' and 'return' statements)
        ReturnVariable rv = new ReturnVariableImpl(methodInfo);
        // start analysis, and copy results of last statement into method
        VariableInfoMap closure = methodInfo.typeInfo().analysis()
                .getOrDefault(VARIABLES_OF_ENCLOSING_METHOD, EMPTY_VARIABLE_INFO_MAP);
        InternalVariables iv = new InternalVariables(rv, closure);
        try {
            VariableData lastOfMainBlock = doBlock(methodInfo, methodBody, null, iv);
            if (lastOfMainBlock != null) {
                if (!methodInfo.analysis().haveAnalyzedValueFor(VariableDataImpl.VARIABLE_DATA)) {
                    methodInfo.analysis().set(VariableDataImpl.VARIABLE_DATA, lastOfMainBlock);
                }
            } // else: empty

            // always escapes
            ComputeAlwaysEscapes.go(methodBody);
        } catch (Throwable t) {
            LOGGER.error("Caught exception in method {}", methodInfo);
            throw t;
        }
    }

    private Map<String, VariableData> doBlocks(MethodInfo methodInfo,
                                               Statement parentStatement,
                                               VariableData vdOfParent,
                                               InternalVariables iv) {
        Stream<Block> blockStream;
        // variables created in catch clauses and record patterns are represented as LVCs
        if (parentStatement instanceof TryStatement ts) {
            blockStream = tryStatementBlockStream(ts);
        } else if (parentStatement instanceof SwitchStatementNewStyle ns) {
            blockStream = newStyleSwitchStatementBlockStream(ns);
        } else {
            blockStream = parentStatement.subBlockStream();
        }
        return blockStream
                .filter(b -> !b.isEmpty())
                .collect(Collectors.toUnmodifiableMap(
                        block -> block.statements().getFirst().source().index(),
                        block -> doBlock(methodInfo, block, vdOfParent, iv)));
    }

    private Stream<Block> newStyleSwitchStatementBlockStream(SwitchStatementNewStyle ns) {
        return ns.entries().stream().map(se -> {
            Block block = se.statementAsBlock();
            if (se.patternVariable() == null || block.isEmpty()) {
                return block;
            }
            Block.Builder bb = runtime.newBlockBuilder();
            se.patternVariable().variableStreamDescend().forEach(v -> {
                LocalVariable lv = (LocalVariable) v;
                if (!lv.isUnnamed()) {
                    // the index of the synthetic statement must be the index of the first statement in the block,
                    // for the merge to work later on
                    LocalVariableCreation lvc = runtime.newLocalVariableCreationBuilder()
                            .setSource(block.statements().getFirst().source())
                            .setLocalVariable(lv.withAssignmentExpression(runtime.newEmptyExpression())).build();
                    bb.addStatement(lvc);
                }
            });
            bb.addStatements(block.statements());
            return bb.build();
        });
    }

    private Stream<Block> tryStatementBlockStream(TryStatement ts) {
        Stream<Block> main;
        if (ts.resources().isEmpty()) {
            main = Stream.of(ts.block());
        } else {
            Block.Builder bb = runtime.newBlockBuilder();
            bb.addStatements(ts.resources());
            bb.addStatements(ts.block().statements());
            main = Stream.of(bb.build());
        }
        Stream<Block> catchStream = ts.catchClauses().stream().map(cc -> {
            if (cc.catchVariable().isUnnamed() || cc.block().isEmpty()) return cc.block();
            Block.Builder bb = runtime.newBlockBuilder();
            LocalVariableCreation lvc = runtime.newLocalVariableCreationBuilder()
                    .setSource(cc.block().statements().getFirst().source())
                    .setLocalVariable(cc.catchVariable().withAssignmentExpression(runtime.newEmptyExpression())).build();
            bb.addStatement(lvc);
            bb.addStatements(cc.block().statements());
            return bb.build();
        });
        return Stream.concat(Stream.concat(main, catchStream), Stream.of(ts.finallyBlock()));
    }

    private VariableData doBlock(MethodInfo methodInfo,
                                 Block block,
                                 VariableData vdOfParent,
                                 InternalVariables iv) {
        VariableData previous = vdOfParent;
        boolean first = true;
        for (Statement statement : block.statements()) {
            try {
                previous = doStatement(methodInfo, statement, previous, first, iv);
                if (first) first = false;
            } catch (Throwable t) {
                LOGGER.error("Caught exception in statement {}", statement.source());
                throw t;
            }
        }
        return previous;
    }

    void doInitializerExpression(FieldInfo fieldInfo) {
        Expression expression = fieldInfo.initializer();
        VariableDataImpl vd = new VariableDataImpl();
        if (!expression.isEmpty()) {
            InternalVariables iv = new InternalVariables();
            Visitor v = new Visitor("0", Set.of(), expression, null, null, null, iv);
            expression.visit(v);
            ReadWriteData readWriteData = new ReadWriteData(null, v.seenFirstTime, v.accessorSeenFirstTime,
                    v.read, v.assigned, v.restrictToScope);
            fromReadWriteDataIntoVd(readWriteData, false, vd, iv,
                    null, null, "0");
        }
        fieldInfo.analysisOfInitializer().set(VariableDataImpl.VARIABLE_DATA, vd);
    }

    private VariableData doStatement(MethodInfo methodInfo,
                                     Statement statement,
                                     VariableData previous,
                                     boolean first,
                                     InternalVariables ivIn) {
        InternalVariables iv = new InternalVariables(ivIn);
        String index = statement.source().index();
        VariableDataImpl vdi = new VariableDataImpl();
        ReadWriteData readWriteData = analyzeEval(methodInfo, previous, vdi, index, statement, iv);
        boolean hasMerge = statement.hasSubBlocks();
        Stage stageOfPrevious = first ? Stage.EVALUATION : Stage.MERGE;

        fromReadWriteDataIntoVd(readWriteData, hasMerge, vdi, iv, previous, stageOfPrevious, index);

        iv.handleStatement(index, statement);

        if (statement instanceof SwitchStatementOldStyle oss) {
            doOldStyleSwitchBlock(methodInfo, oss, vdi, iv);
        } else {
            // sub-blocks
            if (statement.hasSubBlocks()) {
                Map<String, VariableData> lastOfEachSubBlock = doBlocks(methodInfo, statement, vdi, iv);

                boolean noBreakStatementsInside;
                if (statement instanceof LoopStatement) {
                    noBreakStatementsInside = !iv.breakCountsInLoop.containsKey(index);
                } else {
                    noBreakStatementsInside = false;
                }
                addMerge(index, statement, vdi, noBreakStatementsInside, lastOfEachSubBlock, iv, Map.of());
            }
        }
        iv.endHandleStatement(statement);

        statement.analysis().set(VariableDataImpl.VARIABLE_DATA, vdi);
        return vdi;
    }

    private void fromReadWriteDataIntoVd(ReadWriteData readWriteData,
                                         boolean hasMerge,
                                         VariableDataImpl vdi,
                                         InternalVariables iv,
                                         VariableData previousVd,
                                         Stage stageOfPrevious,
                                         String index) {
        readWriteData.seenFirstTime.forEach((v, i) -> {
            VariableInfoContainer vic = initialVariable(i, v, readWriteData, previousVd, iv,
                    hasMerge && localComponents(v).isEmpty());
            vdi.put(v, vic);
            String limitedScope = readWriteData.restrictToScope.get(v);
            if (limitedScope != null) {
                assert iv.parent != null;
                iv.parent.limitedScopeOfPatternVariablesPut(v, i, limitedScope);
            }
        });

        Stream<VariableInfoContainer> streamOfPrevious;
        if (previousVd == null) {
            streamOfPrevious = Stream.of();
        } else {
            streamOfPrevious = previousVd.variableInfoContainerStream();
        }
        streamOfPrevious.forEach(vic -> {
            VariableInfo vi = vic.best(stageOfPrevious);
            String indexOfDefinition = vi.assignments().indexOfDefinition();
            Variable variable = vi.variable();
            VariableData closureVic = iv.closure.get(variable.fullyQualifiedName());
            if (closureVic != null ||
                Util.inScopeOf(indexOfDefinition, index) && iv.acceptLimitedScope(previousVd, vi.variable(), indexOfDefinition, index)) {
                if (!vdi.isKnown(variable.fullyQualifiedName())) {
                    VariableInfoImpl eval = new VariableInfoImpl(variable, readWriteData.assignmentIds(variable, vi),
                            readWriteData.isRead(variable, vi), closureVic);
                    boolean specificHasMerge = hasMerge && !readWriteData.seenFirstTime.containsKey(variable);
                    VariableInfoContainer newVic = new VariableInfoContainerImpl(variable, vic.variableNature(),
                            Either.left(vic), eval, specificHasMerge);
                    vdi.put(variable, newVic);
                } // e.g. a variable from outer shadowed in a lambda
            }
        });

        readWriteData.accessorSeenFirstTime
                .entrySet().stream().filter(e -> !vdi.isKnown(e.getKey().fullyQualifiedName()))
                .forEach(e -> {
                    String i = e.getValue();
                    Variable v = e.getKey();
                    Assignments firstAssigned = new Assignments(i);
                    VariableInfoImpl initial = new VariableInfoImpl(v, firstAssigned, Reads.NOT_YET_READ, null);
                    Reads reads = new Reads(i);
                    VariableInfoImpl eval = new VariableInfoImpl(v, readWriteData.assignmentIds(v, initial), reads, null);
                    VariableInfoContainer vic = new VariableInfoContainerImpl(v, NormalVariableNature.INSTANCE,
                            Either.right(initial), eval, false);
                    vdi.put(v, vic);
                });
    }

    private static final VariableNature SYNTHETIC = new VariableNature() {
    };

    /*
     completely custom block-code
     */
    private void doOldStyleSwitchBlock(MethodInfo methodInfo,
                                       SwitchStatementOldStyle oss,
                                       VariableDataImpl vdOfParent,
                                       InternalVariables iv) {
        Set<String> startOfNewLabels = oss.switchLabelMap().keySet();
        String index = oss.source().index();
        assert vdOfParent != null;
        LocalVariable bv = runtime.newLocalVariable("bv-" + index, runtime.booleanParameterizedType(),
                runtime.newEmptyExpression());
        iv.setBreakVariable(bv);
        Assignments notYetAssigned = new Assignments(index + EVAL);
        VariableInfoImpl vii = new VariableInfoImpl(bv, notYetAssigned, Reads.NOT_YET_READ, null);
        vdOfParent.put(bv, new VariableInfoContainerImpl(bv, SYNTHETIC, Either.right(vii), null,
                false));
        VariableData previous = vdOfParent;
        boolean first = true;
        String indexOfFirstStatement = null;
        Map<String, VariableData> lastOfEachSubBlock = new HashMap<>();

        List<VariableData> fallThrough = new ArrayList<>();
        Map<String, List<VariableData>> fallThroughRecord = new HashMap<>();

        for (Statement statement : oss.block().statements()) {
            String statementIndex = statement.source().index();
            if (indexOfFirstStatement == null) {
                indexOfFirstStatement = statementIndex;
            }
            if (!first && startOfNewLabels.contains(statementIndex)) {
                // fall-through, we'll start again, but append the current data
                assert previous != vdOfParent;
                fallThrough.add(previous);
                indexOfFirstStatement = statementIndex;
                previous = vdOfParent;
                first = true;
            }
            VariableData vd = doStatement(methodInfo, statement, previous, first, iv);
            if (statement instanceof BreakStatement
                || statement instanceof ReturnStatement
                || statementGuaranteedToExit(indexOfFirstStatement, vd, iv)) {
                if (indexOfFirstStatement != null) {
                    lastOfEachSubBlock.put(indexOfFirstStatement, vd);
                    if (!fallThrough.isEmpty()) {
                        fallThroughRecord.put(indexOfFirstStatement, List.copyOf(fallThrough));
                        fallThrough.clear();
                    }
                    indexOfFirstStatement = null;
                }
                first = true;
                previous = vdOfParent;
            } else {
                previous = vd;
                first = false;
            }
        }
        if (indexOfFirstStatement != null) {
            lastOfEachSubBlock.put(indexOfFirstStatement, previous);
        }
        // noBreakStatementsInside irrelevant for switch
        addMerge(index, oss, vdOfParent, false, lastOfEachSubBlock, iv, fallThroughRecord);
    }

    private boolean statementGuaranteedToExit(String index, VariableData vd, InternalVariables iv) {
        for (Variable v : iv.currentVariables()) {
            VariableInfoContainer vic = vd.variableInfoContainerOrNull(v.fullyQualifiedName());
            if (vic != null) {
                VariableInfo vi = vic.best();
                if (vi.assignments().lastAssignmentIsMergeInBlockOf(index)) return true;
            }
        }
        return false;
    }

    /*
    Variables can be local to a block, in which case they are NOT transferred to the merge.
    Fields occurring in one of the blocks are kept in the merge.
    Scopes of fields that and array variables may survive in the following conditions:
    Pattern variables may survive, if they occurred in a "negative" expression: "if(!(e instanceof X x)) { throw ... }"

    Some variables already exist, but are not referenced in any of the blocks at all.
     */
    private void addMerge(String index,
                          Statement statement,
                          VariableDataImpl vdStatement,
                          boolean noBreakStatementsInside,
                          Map<String, VariableData> lastOfEachSubBlock,
                          InternalVariables iv,
                          Map<String, List<VariableData>> fallThroughRecord) {
        Map<Variable, Map<String, VariableInfo>> map = new HashMap<>();
        for (Map.Entry<String, VariableData> entry : lastOfEachSubBlock.entrySet()) {
            String subIndex = entry.getKey();
            VariableData vd = entry.getValue();
            vd.variableInfoStream().forEach(vi -> {
                VariableInfoContainer vic = vdStatement.variableInfoContainerOrNull(vi.variable().fullyQualifiedName());
                if (vic == null || vic.hasMerge()) {
                    if (copyToMerge(index, vi.variable(), vd, iv.closure)
                        && iv.acceptLimitedScope(vd, vi.variable(), vi.assignments().indexOfDefinition(), index)) {
                        map.computeIfAbsent(vi.variable(), v -> new TreeMap<>()).put(subIndex, vi);
                    }
                }
            });
        }
        if (map.isEmpty()) {
            // copy best(EVAL) into merge, if a merge is present
            vdStatement.variableInfoContainerStream().forEach(vic -> {
                if (vic.hasMerge()) {
                    VariableInfoContainerImpl vici = (VariableInfoContainerImpl) vic;
                    vici.setMerge((VariableInfoImpl) vic.best(Stage.EVALUATION));
                }
            });
        } else {
            map.forEach((v, vis) -> {
                Map<String, Assignments> assignmentsPerBlock = vis.entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().assignments()));
                // the return variable passed on for corrections is 'null' when we're computing for the return variable
                ReturnVariable returnVariable = v.equals(iv.rv) ? null : iv.rv;
                Assignments.CompleteMerge assignmentsRequiredForMerge = Assignments.assignmentsRequiredForMerge(statement,
                        lastOfEachSubBlock, returnVariable, noBreakStatementsInside);
                // only old-style switch needs this 'lastOfEachSubBlock; only loops need "noBreakStatementsInside

                Map<String, List<Assignments>> fallThroughForV = computeFallThrough(fallThroughRecord, v);
                Assignments assignments = Assignments.mergeBlocks(index, assignmentsRequiredForMerge, assignmentsPerBlock,
                        fallThroughForV);
                List<String> readIds = vis.values().stream()
                        .flatMap(vi -> vi.reads().indices().stream()).distinct().sorted().toList();
                VariableData closureVi = iv.closure.get(v.fullyQualifiedName());
                VariableInfoImpl merge = new VariableInfoImpl(v, assignments, new Reads(readIds), closureVi);
                VariableInfoContainer inMap = vdStatement.variableInfoContainerOrNull(v.fullyQualifiedName());
                VariableInfoContainerImpl vici;
                if (inMap == null) {
                    String indexOfDefinition = v instanceof LocalVariable ? index : BEFORE_METHOD;
                    Assignments notYetAssigned = new Assignments(indexOfDefinition);
                    VariableInfoImpl initial = new VariableInfoImpl(v, notYetAssigned, Reads.NOT_YET_READ, closureVi);
                    vici = new VariableInfoContainerImpl(v, NormalVariableNature.INSTANCE, Either.right(initial), initial,
                            true);
                    vdStatement.put(v, vici);
                } else {
                    vici = (VariableInfoContainerImpl) inMap;
                }
                vici.setMerge(merge);
            });

            // variables in vdStatement, not in map
            // see TestAssignment,13, source[i-min]
            vdStatement.variableInfoContainerStream()
                    .filter(vic -> !map.containsKey(vic.variable()) && vic.hasEvaluation() && vic.hasMerge())
                    .forEach(vic -> {
                        Variable variable = vic.variable();
                        LOGGER.debug("Copying from E -> M: {}", variable);
                        VariableInfoContainerImpl vici = (VariableInfoContainerImpl) vic;
                        vici.setMerge((VariableInfoImpl) vic.best(Stage.EVALUATION));
                    });
        }
    }

    private static Map<String, List<Assignments>> computeFallThrough(Map<String, List<VariableData>> fallThroughRecord, Variable v) {
        if (fallThroughRecord.isEmpty()) return Map.of();
        Map<String, List<Assignments>> res = new HashMap<>();
        fallThroughRecord.forEach((index, vds) -> vds.forEach(vd -> {
            VariableInfoContainer vic = vd.variableInfoContainerOrNull(v.fullyQualifiedName());
            if (vic != null) {
                VariableInfo vi = vic.best();
                Assignments a = vi.assignments();
                if (!a.isEmpty()) {
                    List<Assignments> list = res.computeIfAbsent(index, ii -> new ArrayList<>());
                    list.add(a);
                }
            }
        }));
        return res;
    }

    private boolean copyToMerge(String index, Variable variable, VariableData vd, VariableInfoMap closure) {
        if (closure.contains(variable.fullyQualifiedName())) return true;
        Set<LocalVariable> localComponents = localComponents(variable);
        if (localComponents.isEmpty()) return true;
        for (LocalVariable localComponent : localComponents) {
            VariableInfoContainer vic = vd.variableInfoContainerOrNull(localComponent.fullyQualifiedName());
            if (vic != null && index.compareTo(vic.best().assignments().indexOfDefinition()) < 0) {
                return false;
            }
        }
        return true;
    }

    public static Set<LocalVariable> localComponents(Variable variable) {
        return variable.variableStreamDescend()
                .filter(v -> v instanceof LocalVariable)
                .map(v -> (LocalVariable) v)
                .collect(Collectors.toUnmodifiableSet());
    }

    private VariableInfoContainer initialVariable(String index,
                                                  Variable v,
                                                  ReadWriteData readWriteData,
                                                  VariableData previousVd,
                                                  InternalVariables iv,
                                                  boolean hasMerge) {
        String indexOfDefinition = indexOfDefinition(v, index, previousVd, iv);
        Assignments notYetAssigned = new Assignments(indexOfDefinition);
        VariableData viInClosure = iv.closure.get(v.fullyQualifiedName());
        VariableInfoImpl initial = new VariableInfoImpl(v, notYetAssigned, Reads.NOT_YET_READ, viInClosure);
        Reads reads = readWriteData.isRead(v, initial);
        VariableInfoImpl eval = new VariableInfoImpl(v, readWriteData.assignmentIds(v, initial), reads, viInClosure);
        return new VariableInfoContainerImpl(v, NormalVariableNature.INSTANCE,
                Either.right(initial), eval, hasMerge);
    }

    private String indexOfDefinition(Variable v, String index, VariableData previousVd, InternalVariables iv) {
        if (iv.closure.contains(v.fullyQualifiedName())) {
            return StatementIndex.ENCLOSING_METHOD;
        }
        if (v instanceof LocalVariable) {
            // NEW!
            return index;
        }
        return recursiveIndexOfDefinition(v, index, previousVd, iv);
    }

    private String recursiveIndexOfDefinition(Variable v, String index, VariableData previousVd, InternalVariables iv) {
        if (v == null) return BEFORE_METHOD;
        if (iv.closure.contains(v.fullyQualifiedName())) {
            return StatementIndex.ENCLOSING_METHOD;
        }
        VariableInfoContainer viPrevious = previousVd != null ? previousVd.variableInfoContainerOrNull(v.fullyQualifiedName()) : null;
        if (viPrevious != null) {
            VariableInfo recursiveInitialOrNull = viPrevious.getRecursiveInitialOrNull();
            if (recursiveInitialOrNull != null) {
                return recursiveInitialOrNull.assignments().indexOfDefinition();
            }
        }
        if (v instanceof DependentVariable dv) {
            String a = recursiveIndexOfDefinition(dv.arrayVariable(), index, previousVd, iv);
            String i = recursiveIndexOfDefinition(dv.indexVariable(), index, previousVd, iv);
            return a.compareTo(i) > 0 ? a : i;
        }
        if (v instanceof FieldReference fr) {
            return recursiveIndexOfDefinition(fr.fieldReferenceBase(), index, previousVd, iv);
        }
        //assert !(v instanceof LocalVariable);
        // This, ClassVariable, ParameterInfo
        return BEFORE_METHOD;
    }

    private ReadWriteData analyzeEval(MethodInfo methodInfo,
                                      VariableData previous,
                                      VariableData current,
                                      String indexIn, Statement statement, InternalVariables iv) {
        String index;
        if (statement.hasSubBlocks()) {
            String suffix = statement instanceof DoStatement ? EVAL_UPDATE : EVAL;
            index = indexIn + suffix;
        } else {
            index = indexIn;
        }
        Set<String> knownVariableNames = previous == null ? Set.of() : previous.knownVariableNames();
        Visitor v = new Visitor(index, knownVariableNames, statement, previous, current, methodInfo, iv);
        boolean eval = true;
        if (statement instanceof ReturnStatement || statement instanceof ThrowStatement) {
            v.assignedAdd(iv.rv);
            if (!v.knownVariableNames.contains(iv.rv.fullyQualifiedName())) {
                v.seenFirstTime.put(iv.rv, v.index);
            }
        } else {
            LocalVariable bv = iv.bv();
            if (statement instanceof BreakStatement && bv != null) {
                v.assignedAdd(bv);
                if (!v.knownVariableNames.contains(bv.fullyQualifiedName())) {
                    v.seenFirstTime.put(bv, v.index);
                }
            } else if (statement instanceof LocalVariableCreation lvc) {
                handleLvc(lvc, v);
            } else if (statement instanceof ForStatement fs) {
                for (Element initializer : fs.initializers()) {
                    if (initializer instanceof LocalVariableCreation lvc) {
                        handleLvc(lvc, v.withIndex(indexIn + EVAL_INIT));
                    } else if (initializer instanceof Expression e) {
                        e.visit(v.withIndex(indexIn + EVAL_INIT));
                    } else throw new UnsupportedOperationException();
                }
                for (Expression updater : fs.updaters()) {
                    updater.visit(v.withIndex(indexIn + EVAL_UPDATE));
                }
                // the condition is evaluated 2x, but we'll have to do the normal one first!!
                // (see e.g. TestModificationArrays in linkedvariables)
                fs.expression().visit(v.withIndex(indexIn + EVAL));
                fs.expression().visit(v.withIndex(indexIn + EVAL_AFTER_UPDATE));
                eval = false;
            } else if (statement instanceof WhileStatement ws) {
                ws.expression().visit(v.withIndex(indexIn + EVAL_AFTER_UPDATE));
            } else if (statement instanceof ForEachStatement fe) {
                handleLvc(fe.initializer(), v.withIndex(indexIn + EVAL_INIT));
                v.assignedAdd(fe.initializer().localVariable());
            } else if (statement instanceof AssertStatement as) {
                if (as.message() != null) as.message().visit(v);
            } else if (statement instanceof ExplicitConstructorInvocation eci) {
                eci.parameterExpressions().forEach(e -> e.visit(v));
            } else if (statement instanceof LocalTypeDeclaration ltd) {
                v.handleInnerClass(ltd.typeInfo());
                eval = false;
            }
        }
        if (eval) {
            Expression expression = statement.expression();
            if (expression != null && !expression.isEmpty()) expression.visit(v.withIndex(index));
        }
        return new ReadWriteData(previous, v.seenFirstTime, v.accessorSeenFirstTime, v.read, v.assigned,
                v.restrictToScope);
    }

    private static void handleLvc(LocalVariableCreation lvc, Visitor v) {
        v.seenFirstTime.put(lvc.localVariable(), v.index);
        Expression assignmentExpression = lvc.localVariable().assignmentExpression();
        if (!assignmentExpression.isEmpty()) {
            v.assignedAdd(lvc.localVariable());
            assignmentExpression.visit(v);
        }
        for (LocalVariable lv : lvc.otherLocalVariables()) {
            v.seenFirstTime.put(lv, v.index);
            if (!lv.assignmentExpression().isEmpty()) {
                v.assignedAdd(lv);
                lv.assignmentExpression().visit(v);
            }
        }
    }

    private class Visitor implements org.e2immu.language.cst.api.element.Visitor {
        String index;
        int inNegative;

        final Map<Variable, List<String>> read = new HashMap<>();
        final Map<Variable, List<String>> assigned = new HashMap<>();
        final Map<Variable, String> seenFirstTime = new HashMap<>();
        final Map<Variable, String> accessorSeenFirstTime = new HashMap<>();
        final Map<Variable, String> restrictToScope = new HashMap<>();
        final Set<String> knownVariableNames;
        final Element statement;
        final VariableData previousVariableData;
        final VariableData currentVariableData;
        final MethodInfo currentMethod;
        final InternalVariables internalVariables;

        Visitor(String index, Set<String> knownVariableNames, Element statement, VariableData previousVariableData,
                VariableData currentVariableData, MethodInfo currentMethod, InternalVariables internalVariables) {
            this.index = index;
            this.previousVariableData = previousVariableData;
            this.currentVariableData = currentVariableData;
            this.knownVariableNames = Set.copyOf(knownVariableNames); // make sure we do not modify it
            this.statement = statement;
            this.currentMethod = currentMethod;
            this.internalVariables = internalVariables;
        }

        public void assignedAdd(Variable variable) {
            List<String> indices = assigned.computeIfAbsent(variable, v -> new ArrayList<>());
            String last;
            if (!indices.isEmpty() && stripPlus(last = indices.get(indices.size() - 1)).equals(index)) {
                // we've already added one, but there may be others, see TestAssignmentsExpression
                // dedicated code to deal with multiple reassignments in one statement; this is pretty rare!
                int plus = last.lastIndexOf('+');
                if (plus < 0) {
                    indices.add(index + "+0");
                } else {
                    indices.add(index + "+" + (1 + Integer.parseInt(last.substring(plus + 1))));
                }
            } else {
                indices.add(index);
            }
            assert variable != null;
            if (!knownVariableNames.contains(variable.fullyQualifiedName()) && !seenFirstTime.containsKey(variable)) {
                seenFirstTime.put(variable, index);
            }
        }

        private static String stripPlus(String s) {
            int plus = s.lastIndexOf('+');
            return plus < 0 ? s : s.substring(0, plus);
        }

        @Override
        public void afterExpression(Expression e) {
            if (e instanceof Negation || e instanceof UnaryOperator u && u.parameterizedType().isBoolean()) {
                --inNegative;
            }
        }

        @Override
        public boolean beforeExpression(Expression expressionIn) {
            Expression e = expressionIn.translate(applyGetSetTranslation);
            if (e instanceof Lambda lambda) {
                // we plan to catch all variables that we already know, but not to introduce NEW variables
                VariableInfoMap variableInfoMap = ensureLocalVariableNamesOfEnclosingType(lambda.methodInfo().typeInfo());
                if (!prepAnalyzer.options.doNotRecurseIntoAnonymous()) {
                    prepAnalyzer.doType(lambda.methodInfo().typeInfo());
                }
                copyReadsFromAnonymousMethod(lambda.methodInfo(), Set.of(), Set.of(lambda.methodInfo().typeInfo()),
                        variableInfoMap);
                return false;
            }

            if (e instanceof ConstructorCall cc) {
                // important: check anonymous type first, it can have constructor != null
                TypeInfo anonymousClass = cc.anonymousClass();
                if (anonymousClass != null) {
                    handleInnerClass(anonymousClass);
                    return true; // so that the arguments get processed; the current visitor ignores the anonymous class
                }
                if (cc.constructor() != null && cc.constructor().isSyntheticArrayConstructor()) {
                    prepAnalyzer.handleSyntheticArrayConstructor(cc);
                }
                if (prepAnalyzer.trackObjectCreations()) {
                    ObjectCreationVariable ocv = new ObjectCreationVariableImpl(currentMethod, cc.source().compact(), cc.parameterizedType());
                    assignedAdd(ocv);
                }
            }
            if (e instanceof Negation || e instanceof UnaryOperator u && u.parameterizedType().isBoolean()) {
                ++inNegative;
                return true;
            }

            if (e instanceof VariableExpression ve) {
                Variable variable = ve.variable();
                markRead(variable);
                if (ve.variable() instanceof DependentVariable dv) {
                    dv.indexExpression().visit(this);
                    dv.arrayExpression().visit(this);
                } else if (ve.variable() instanceof FieldReference fr) {
                    fr.scope().visit(this);
                }
                return false;
            }

            if (e instanceof InstanceOf instanceOf) {
                String scope = computePatternVariableScope();
                if (instanceOf.patternVariable() != null) {
                    RecordPattern recordPattern = instanceOf.patternVariable();
                    processRecordPattern(recordPattern, scope);
                }
                if (instanceOf.expression() instanceof VariableExpression
                    && !instanceOf.analysis().haveAnalyzedValueFor(PropertyImpl.INSTANCEOF_SCOPE)) {
                    instanceOf.analysis().set(PropertyImpl.INSTANCEOF_SCOPE, new ValueImpl.ScopeImpl(scope));
                }
            }
            if (e instanceof Assignment a) {
                assignedAdd(a.variableTarget());
                if (a.assignmentOperator() != null) {
                    // +=, ++, ...
                    markRead(a.variableTarget());
                }
                if (a.variableTarget() instanceof DependentVariable dv) {
                    dv.indexExpression().visit(this);
                    dv.arrayExpression().visit(this);
                    if (dv.arrayVariable() != null) {
                        markRead(dv.arrayVariable());
                    }
                    if (dv.indexVariable() != null) {
                        markRead(dv.indexVariable());
                    }
                } else if (a.variableTarget() instanceof FieldReference fr) {
                    fr.scope().visit(this);
                }
                a.value().visit(this);
                return false;
            }
            if (e instanceof MethodCall mc) {
                // also, simply ensure that modified component variables exist
                copyModifiedComponentsMethod(mc.methodInfo(), mc.object());
            }
            if (e instanceof SwitchExpression switchExpression) {
                switchExpression.selector().visit(this);
                for (SwitchEntry se : switchExpression.entries()) {
                    if (se.statement() instanceof Block block) {
                        doBlock(currentMethod, block, currentVariableData, internalVariables);
                    } else {
                        doStatement(currentMethod, se.statement(), currentVariableData, true, internalVariables);
                    }
                }
                return false;
            }
            if (e instanceof CommaExpression ce) {
                for (Expression comma : ce.expressions()) {
                    comma.visit(this);
                }
                return false;
            }
            return true;
        }

        private void processRecordPattern(RecordPattern recordPattern, String scope) {
            if (recordPattern.localVariable() != null) {
                LocalVariable lv = recordPattern.localVariable();
                seenFirstTime.put(lv, index);
                assignedAdd(lv);
                restrictToScope.put(lv, scope);
            } else if (recordPattern.recordType() != null) {
                for (RecordPattern rp : recordPattern.patterns()) {
                    processRecordPattern(rp, scope);
                }
            }
        }

        private void handleInnerClass(TypeInfo innerClass) {
            VariableInfoMap variableInfoMap = ensureLocalVariableNamesOfEnclosingType(innerClass);
            if (!prepAnalyzer.options.doNotRecurseIntoAnonymous()) {
                prepAnalyzer.doType(innerClass);
            }

            Set<FieldInfo> localFields = new HashSet<>(innerClass.fields());
            Set<TypeInfo> typeHierarchy = new HashSet<>();
            typeHierarchy.add(innerClass);
            typeHierarchy.addAll(innerClass.superTypesExcludingJavaLangObject());
            typeHierarchy.add(runtime.objectTypeInfo());
            innerClass.superTypesExcludingJavaLangObject().forEach(st -> localFields.addAll(st.fields()));
            innerClass.fields().forEach(f -> {
                if (f.initializer() != null && !f.initializer().isEmpty()) {
                    copyReadsFromAnonymousMethod(f.analysisOfInitializer(), localFields, typeHierarchy,
                            variableInfoMap);
                }
            });
            innerClass.constructorAndMethodStream()
                    .forEach(mi -> copyReadsFromAnonymousMethod(mi, localFields, typeHierarchy,
                            variableInfoMap));
        }

        // This annotation only exists here through @Modified("...") annotations; not through analysis, which comes later
        private void copyModifiedComponentsMethod(MethodInfo methodInfo, Expression object) {
            Value.VariableBooleanMap modifiedComponents = methodInfo.analysis()
                    .getOrDefault(PropertyImpl.MODIFIED_COMPONENTS_METHOD, ValueImpl.VariableBooleanMapImpl.EMPTY);
            modifiedComponents.map().keySet().forEach(v -> {
                if (v instanceof FieldReference fr) {
                    FieldReference newFr = runtime.newFieldReference(fr.fieldInfo(), object, fr.parameterizedType());
                    markRead(newFr);
                }
            });
        }

        private VariableInfoMap ensureLocalVariableNamesOfEnclosingType(TypeInfo anonymousClass) {
            VariableInfoMap stored = anonymousClass.analysis().getOrNull(VARIABLES_OF_ENCLOSING_METHOD,
                    VariableInfoMap.class);
            if (stored == null) {
                Set<String> set = allKnownLocalVariableNames();
                VariableInfoMap variableInfoMap = new VariableInfoMap(set, currentVariableData);
                anonymousClass.analysis().set(VARIABLES_OF_ENCLOSING_METHOD, variableInfoMap);
                return variableInfoMap;
            }
            return stored;
        }

        private Set<String> allKnownLocalVariableNames() {
            if (previousVariableData != null) {
                return previousVariableData.variableInfoContainerStream()
                        .map(VariableInfoContainer::bestCurrentlyComputed)
                        .filter(vi -> vi != null && !localComponents(vi.variable()).isEmpty())
                        .map(vi -> vi.variable().fullyQualifiedName())
                        .collect(Collectors.toUnmodifiableSet());
            }
            return Set.of();
        }

        private void copyReadsFromAnonymousMethod(MethodInfo methodInfo,
                                                  Set<FieldInfo> localFields,
                                                  Set<TypeInfo> typeHierarchy,
                                                  VariableInfoMap closure) {
            Statement last = methodInfo.methodBody().lastStatement();
            if (last != null) {
                copyReadsFromAnonymousMethod(last.analysis(), localFields, typeHierarchy, closure);
            }
        }

        private void copyReadsFromAnonymousMethod(PropertyValueMap analysis,
                                                  Set<FieldInfo> localFields,
                                                  Set<TypeInfo> typeHierarchy,
                                                  VariableInfoMap closure) {
            VariableData vd = analysis.getOrNull(VariableDataImpl.VARIABLE_DATA, VariableDataImpl.class);
            vd.variableInfoStream().forEach(vi -> {
                Variable v = vi.variable();
                boolean accept = acceptForCopy(v, localFields, typeHierarchy, closure);
                if (accept && !vi.reads().isEmpty()) {
                    markRead(v);
                }
            });
        }

        private static boolean acceptForCopy(Variable v,
                                             Set<FieldInfo> localFields,
                                             Set<TypeInfo> typeHierarchy,
                                             VariableInfoMap closure) {
            if (v instanceof LocalVariable) {
                return closure.contains(v.fullyQualifiedName());
            }
            if (v instanceof ParameterInfo pi) {
                return !typeHierarchy.contains(pi.methodInfo().typeInfo());
            }
            if (v instanceof This thisVar) {
                return !typeHierarchy.contains(thisVar.typeInfo());
            }
            if (v instanceof ReturnVariable) {
                return false;
            }
            if (v instanceof FieldReference fr) {
                return !localFields.contains(fr.fieldInfo())
                       && (fr.scopeVariable() == null || acceptForCopy(fr.scopeVariable(), localFields, typeHierarchy, closure));
            }
            if (v instanceof DependentVariable dv) {
                return (dv.arrayVariable() == null || acceptForCopy(dv.arrayVariable(), localFields, typeHierarchy, closure))
                       && (dv.indexVariable() == null || acceptForCopy(dv.indexVariable(), localFields, typeHierarchy, closure));
            }
            throw new UnsupportedOperationException();
        }

        private void markRead(Variable v) {
            read.computeIfAbsent(v, vv -> new ArrayList<>()).add(index);
            if (!knownVariableNames.contains(v.fullyQualifiedName()) && !seenFirstTime.containsKey(v)) {
                seenFirstTime.put(v, index);
            }
        }

        /*
            create local variables 'YYY y = x' for every sub-expression x instanceof YYY y

            the scope of the variable is determined as follows:
            (1) if the expression is an if-statement, without else: pos = then block, neg = rest of current block
            (2) if the expression is an if-statement with else: pos = then block, neg = else block
            (3) otherwise, only the current expression is accepted (we set to then block)
            ==> positive: always then block
            ==> negative: either else or rest of block
        */

        private String computePatternVariableScope() {
            boolean isNegative = (inNegative % 2) == 1;
            String stmtIndex = statement.source().index();
            if (statement instanceof IfElseStatement ie) {
                if (isNegative) {
                    if (ie.elseBlock().isEmpty()) {
                        // rest of the current block
                        return Util.beyond(stmtIndex);
                    }
                    return stmtIndex + ".1.0";
                }
            }
            // then block, or restrict to current statement
            return stmtIndex + ".0.0";
        }

        public Visitor withIndex(String s) {
            this.index = s;
            return this;
        }
    }
}
