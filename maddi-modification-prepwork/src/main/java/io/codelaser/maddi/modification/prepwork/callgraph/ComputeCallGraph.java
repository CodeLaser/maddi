/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.modification.prepwork.callgraph;


import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.JavaDoc;
import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.TryStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.inspection.api.byname.ByNameDangling;
import io.codelaser.maddi.inspection.api.byname.ByNameReference;
import io.codelaser.maddi.inspection.api.byname.ByNameSink;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.ImmutableGraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static io.codelaser.maddi.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;

/*
call & reference graphs.

direction of arrow: I need you to exist first (I, from -> you, to)
 */
public class ComputeCallGraph {
    public static final Property RECURSIVE_METHOD = new PropertyImpl("recursiveMethod", ValueImpl.BoolImpl.FALSE);
    private final Runtime runtime;
    private final Set<TypeInfo> primaryTypes;
    private final Set<MethodInfo> recursive = new HashSet<>();
    // ⛔ mergeWeights, NOT Long::sum: the weight is six packed counters, and a carry out of one is a different KIND
    // of edge rather than a bigger count. See the lane layout above.
    private final G.Builder<Info> builder = new ImmutableGraph.Builder<>(ComputeCallGraph::mergeWeights);
    private final Predicate<TypeInfo> externalsToAccept;
    private final Collection<ModuleInfo> moduleInfos;

    /*
    THE EDGE WEIGHT IS SIX COUNTERS PACKED INTO ONE long, in ascending order of strength:

        bits 48-63  CODE_STRUCTURE        S
        bits 40-47  TYPE_HIERARCHY        H
        bits 32-39  TYPES_IN_DECLARATION  D
        bits 16-31  REFERENCES            R   <- isAtLeastReference()'s threshold
        bits  8-15  BY_NAME_REFERENCES    n   } SOFT: below the threshold, so invisible to every consumer
        bits  0- 7  DOC_REFERENCES        d   } that filters with isAtLeastReference / isReference

    ⭐ The two soft lanes are references the COMPILER does not see and an EDITOR must still update: a javadoc link,
    and a type or member named by a string literal that a by-name sink resolves (Class.forName and friends). They
    sit below the threshold on purpose -- adding them to the type graph would put arcs into every cycle, giant and
    layering the campaign measures -- and a consumer that wants them asks for them by name.

    ⛔ THE LANES ARE 8 BITS EACH, SO THEY SATURATE RATHER THAN CARRY; see mergeWeights. They used to be one 16-bit
    doc lane, and the merge was Long::sum, which meant a 65,536th doc reference would have become one phantom
    REFERENCE. Nothing has ever come close, but the failure mode is silent and the fix is one operator.
     */
    private static final long CODE_STRUCTURE_BITS = 48;
    public static final long CODE_STRUCTURE = 1L << CODE_STRUCTURE_BITS;
    private static final long TYPE_HIERARCHY_BITS = 40;
    public static final long TYPE_HIERARCHY = 1L << TYPE_HIERARCHY_BITS;
    private static final long TYPES_IN_DECLARATION_BITS = 32;
    public static final long TYPES_IN_DECLARATION = 1L << TYPES_IN_DECLARATION_BITS;
    private static final long REFERENCES_BITS = 16;
    public static final long REFERENCES = 1L << REFERENCES_BITS;
    private static final long BY_NAME_REFERENCES_BITS = 8;
    public static final long BY_NAME_REFERENCES = 1L << BY_NAME_REFERENCES_BITS;
    public static final long DOC_REFERENCES = 1;

    /**
     * The first value of each lane, lowest first, terminated by 0 — which is the next power of two after
     * {@code CODE_STRUCTURE}'s lane, modulo 2^64, so the top lane's mask needs no special case.
     */
    private static final long[] LANES = {DOC_REFERENCES, BY_NAME_REFERENCES, REFERENCES, TYPES_IN_DECLARATION,
            TYPE_HIERARCHY, CODE_STRUCTURE, 0};

    /**
     * Add {@code a} and {@code b} lane by lane, each lane saturating at its own maximum instead of carrying into
     * the lane above. <b>This is the graph builder's merge operator</b>, in place of {@code Long::sum}: a carry out
     * of a counter is not a bigger count, it is a different KIND of edge, and every consumer reads the kind.
     */
    public static long mergeWeights(long a, long b) {
        long result = 0;
        for (int i = 0; i + 1 < LANES.length; i++) {
            long mask = LANES[i + 1] - LANES[i]; // 0 - CODE_STRUCTURE is the top lane's mask, unsigned
            long sum = (a & mask) + (b & mask);
            result |= Long.compareUnsigned(sum, mask) > 0 ? mask : sum;
        }
        return result;
    }

    private G<Info> graph;

    // by-name recognition is OFF until a caller declares sinks: with an empty list nothing below runs, no row is
    // produced and no bit of the BY_NAME lane is ever set, so every existing maddi user sees the graph it saw
    private List<ByNameSink> byNameSinks = List.of();
    private ParseResult byNameParseResult;
    private final List<ByNameReference> byNameReferences = new ArrayList<>();
    private final List<ByNameDangling> byNameDanglings = new ArrayList<>();
    private int unresolvedSinkCalls;

    /**
     * Turn on by-name recognition: every call to one of {@code sinks} whose class argument reads as a binary name
     * that {@code parseResult} resolves becomes a {@link ByNameReference} and a {@link #BY_NAME_REFERENCES} edge.
     * Call before {@link #go()}. Both arguments are required; either absent leaves the feature off.
     */
    public ComputeCallGraph withByNameSinks(List<ByNameSink> sinks, ParseResult parseResult) {
        this.byNameSinks = sinks == null || parseResult == null ? List.of() : List.copyOf(sinks);
        this.byNameParseResult = parseResult;
        return this;
    }

    /** Every place a string literal named a type, in source order per member. Empty unless sinks were declared. */
    /**
     * Literals that read perfectly and resolve to nothing in this parse. Most are healthy (a JDK class, another
     * project); a gate judges which are not. See {@link ByNameDangling} for why an absence had to be turned into
     * a positive record.
     */
    public List<ByNameDangling> byNameDanglings() {
        return List.copyOf(byNameDanglings);
    }

    public List<ByNameReference> byNameReferences() {
        return List.copyOf(byNameReferences);
    }

    /**
     * How many calls to a declared sink had a class argument this could not read as a name — a concatenation, a
     * parameter, a method call, a constant it could not follow. <b>The blind spot as a number.</b> A recogniser
     * that cannot say how much it missed is indistinguishable from one that found everything.
     */
    public int unresolvedSinkCalls() {
        return unresolvedSinkCalls;
    }

    public ComputeCallGraph(Runtime runtime, TypeInfo primaryType) {
        this(runtime, Set.of(primaryType), Set.of(), t -> false);
    }

    public ComputeCallGraph(Runtime runtime,
                            ParseResult parseResult,
                            Predicate<TypeInfo> externalsToAccept) {
        this.runtime = runtime;
        this.primaryTypes = parseResult.primaryTypes();
        this.externalsToAccept = externalsToAccept;
        this.moduleInfos = parseResult.sourceSetToModuleInfoMap().values();
    }

    public ComputeCallGraph(Runtime runtime,
                            Set<TypeInfo> primaryTypes,
                            Collection<ModuleInfo> moduleInfos,
                            Predicate<TypeInfo> externalsToAccept) {
        this.runtime = runtime;
        this.primaryTypes = primaryTypes;
        this.externalsToAccept = externalsToAccept;
        this.moduleInfos = moduleInfos;
    }

    public static boolean isAtLeastReference(long value) {
        return value >= REFERENCES;
    }

    public static boolean isReference(long value) {
        return (value & (TYPES_IN_DECLARATION - 1)) >= REFERENCES;
    }

    public static int referenceCount(long value) {
        return (int) ((value & (TYPES_IN_DECLARATION - 1)) >> REFERENCES_BITS);
    }

    public static int docReferenceCount(long value) {
        return (int) (value & (BY_NAME_REFERENCES - 1));
    }

    /** How many times this edge's {@code from} names its {@code to} by NAME — in a string literal a by-name sink
     * resolves. Soft, like the doc count: below {@link #isAtLeastReference}'s threshold. */
    public static int byNameReferenceCount(long value) {
        return (int) ((value & (REFERENCES - 1)) >> BY_NAME_REFERENCES_BITS);
    }

    public static boolean isByName(long value) {
        return byNameReferenceCount(value) > 0;
    }

    public static int declarationCount(long value) {
        return (int) ((value & (TYPE_HIERARCHY - 1)) >> TYPES_IN_DECLARATION_BITS);
    }

    public static int hierarchyCount(long value) {
        return (int) ((value & (CODE_STRUCTURE - 1)) >> TYPE_HIERARCHY_BITS);
    }

    public static int codeStructureCount(long value) {
        return (int) (value >>> CODE_STRUCTURE_BITS);
    }

    public static String print(G<Info> graph) {
        return graph.toString(", ", ComputeCallGraph::edgeValuePrinter);
    }

    public static String edgeValuePrinter(long value) {
        StringBuilder sb = new StringBuilder();
        if (value >= CODE_STRUCTURE) sb.append("S");
        if ((value & (CODE_STRUCTURE - 1)) >= TYPE_HIERARCHY) sb.append("H");
        if ((value & (TYPE_HIERARCHY - 1)) >= TYPES_IN_DECLARATION) sb.append("D");
        if ((value & (TYPES_IN_DECLARATION - 1)) >= REFERENCES) sb.append("R");
        if (byNameReferenceCount(value) > 0) sb.append("n");
        if (docReferenceCount(value) > 0) sb.append("d");
        return sb.toString();
    }

    /**
     * ⚠ <b>The BY_NAME count is deliberately absent, and not by oversight.</b> This is the clustering weight — how
     * strongly two vertices belong together — and a by-name reference is a compile-time NON-dependency: joining it
     * silently to the sum would move types across a partition on the strength of a string. A caller that wants it
     * adds {@link #byNameReferenceCount} itself, where the choice is visible.
     */
    public static int weightedSumInteractions(long l, int docsWeight, int refsWeight, int declarationWeight,
                                              int hierarchyWeight, int codeStructureWeight) {
        // the doc count is the lowest lane; this read `l & REFERENCES`, the lowest bit of the REFERENCE count, so an
        // odd number of references weighed 65536 docs. Every production caller passed docsWeight 0.
        return docReferenceCount(l) * docsWeight + referenceCount(l) * refsWeight
               + declarationCount(l) * declarationWeight + hierarchyCount(l) * hierarchyWeight
               + codeStructureCount(l) * codeStructureWeight;
    }

    public ComputeCallGraph go() {
        primaryTypes.forEach(this::go);
        moduleInfos.forEach(this::go);
        graph = builder.build();
        return this;
    }

    public G<Info> graph() {
        return graph;
    }

    public Set<MethodInfo> recursiveMethods() {
        return recursive;
    }

    public void setRecursiveMethods() {
        recursive.forEach(mi -> {
            if (!mi.analysis().haveAnalyzedValueFor(RECURSIVE_METHOD)) {
                mi.analysis().set(RECURSIVE_METHOD, TRUE);
            }
        });
    }

    /*
    Edge types:

    CODE STRUCTURE (follows the AST, cannot cause a cycle)

    A. from type to its methods, fields, enclosing types. From a method or field into its anonymous, lambda, local types.

    TYPE HIERARCHY (can cause cycles together with A, very unwanted)

    B. from a type to its ancestors

    TYPE REFERENCES in DECLARATION, except for hierarchy

    C. from a type to its method parameters,
       from method/constructor/field to any type referenced in it, different from the owner (see A, other direction)
        this includes the types of method parameters

    TYPE, METHOD AND FIELD REFERENCES

    D. from method body/field initializer to any type, method or field referenced (as method call, constructor call,
       method reference).

    !! E. THE ONE INVERTED EDGE — read this before consuming the graph !!

       When a method reads or writes a field OF ITS OWN TYPE, the edge is recorded BACKWARDS:

           field -> method          NOT   method -> field

       (see handleFieldAccess). Every other reference in D points from the referring member to what it
       references; this one does not. It is historical, it is deliberate, and it is load-bearing: the
       analysis order must visit a field's value before the methods that consume it, so the arrow
       encodes "the field's value flows into the method", not "the method depends on the field".

       Consequence for anyone walking this graph: iterating only the OUTGOING edges of a member will
       silently miss every access it makes to its own type's state. There is no error and no gap in the
       vertex set — the relation is simply not where you looked. If you need "what does this member
       touch", you must also scan incoming edges from FieldInfo vertices owned by the member's own type.
       Two consumers have been caught by this.

       Note the asymmetry is only for the OWNING type: a method reading another type's field produces the
       ordinary method -> field edge, so the same walk gets self-access and foreign access from opposite
       directions.

       A related trap, different mechanism, same reader: a lambda or anonymous class is its own TypeInfo
       vertex (see A), so ITS outgoing edges are attributed to that synthetic type and not to the method
       it is written inside. A member-level walk has to fold them back into the enclosing method
       (TypeInfo.enclosingMethod), or every dependency introduced by a lambda looks like a dependency of
       the type declaration.

    FIXME
        we don't want types referenced as static type expressions, or types of 'this' when represents the type itself

    Calls from a lambda/anonymous class inside method M to M are marked as recursive. No edge in the graph will be generated.
    Reason: we're already generating an edge from M into the anonymous type (B), from the anonymous type to the method (A)
     */
    private void go(TypeInfo typeInfo) {
        builder.addVertex(typeInfo);

        doJavadoc(typeInfo);
        typeInfo.subTypes().forEach(st -> {
            builder.mergeEdge(typeInfo, st, CODE_STRUCTURE); // A
            go(st);
        });

        typeInfo.interfacesImplemented().forEach(pt -> addType(typeInfo, pt, TYPE_HIERARCHY)); // B
        if (typeInfo.parentClass() != null) addType(typeInfo, typeInfo.parentClass(), TYPE_HIERARCHY); // B
        // a sealed type's 'permits' names its subclasses: a real parent->child reference (and a compile-time
        // dependency). addType() would drop it -- the child is assignable to the parent, so addType's
        // self/supertype guard fires -- so add the hierarchy edge directly, like mergeEdge does elsewhere.
        typeInfo.permittedWhenSealed().forEach(child -> {
            if (child != typeInfo && accept(child)) builder.mergeEdge(typeInfo, child, TYPE_HIERARCHY); // B
        });
        typeInfo.typeParameters().forEach(tp -> tp.typeBounds()
                .forEach(pt -> addType(typeInfo, pt, TYPES_IN_DECLARATION))); // C
        doAnnotations(typeInfo, TYPES_IN_DECLARATION);
        typeInfo.constructorAndMethodStream().forEach(mi -> {
            doJavadoc(mi);
            doAnnotations(mi, TYPES_IN_DECLARATION);
            mi.exceptionTypes().forEach(pt -> addType(mi, pt, TYPES_IN_DECLARATION)); // C
            mi.parameters().forEach(pi -> {
                doAnnotations(pi, TYPES_IN_DECLARATION);
                addType(mi, pi.parameterizedType(), TYPES_IN_DECLARATION);
            }); // C
            if (mi.hasReturnValue()) { // C
                addType(mi, mi.returnType(), TYPES_IN_DECLARATION); // needed because of immutable computation in independent
            }

            builder.mergeEdge(typeInfo, mi, CODE_STRUCTURE); // A
            for (MethodInfo override : mi.overrides()) {
                if (accept(override.typeInfo())) builder.mergeEdge(mi, override, CODE_STRUCTURE);
            }
            Visitor visitor = new Visitor(mi);
            // a half-built method (dual-identity family, task #33: forward-created member of an anonymous
            // class) can arrive without a body; skip rather than NPE — the type will be isolated downstream
            if (mi.methodBody() != null) mi.methodBody().visit(visitor); // D
            doRecordedReferences(mi); // D, after the body: see there
        });
        typeInfo.fields().forEach(fi -> {
            doJavadoc(fi);
            doAnnotations(fi, TYPES_IN_DECLARATION);
            addType(fi, fi.type(), TYPES_IN_DECLARATION); // C
            builder.mergeEdge(typeInfo, fi, CODE_STRUCTURE); // A
            if (fi.initializer() != null && !fi.initializer().isEmpty()) {
                Visitor visitor = new Visitor(fi);
                fi.initializer().visit(visitor); // D
            }
            doRecordedReferences(fi);
        });
        doRecordedReferences(typeInfo);
    }

    /*
    D for the references a front-end RECORDED on a member's source instead of representing as elements
    (DetailedSources.putReference). The Kotlin front-end records every project reference written in a member
    there, because its desugared CST drops many of them: an import, an annotation argument, a call inside a lambda
    passed to a library function. Without these edges the graph -- and every "who refers to X" answered from it --
    would miss exactly the references the CST lost.

    Runs after the member's own elements were visited, and adds an edge only where they did not: the builder
    SUMS weights, so a reference present both as an element and as a record would count as two call sites. For a
    method or field target the test is the R bit; for a type, any edge at all, since a type named in a signature
    or a supertype list already has its D or H edge and naming it is not a use. Same rules as the elements
    otherwise: recursion via handleMethodCall, the inverted own-field edge (E).
     */
    private void doRecordedReferences(Info from) {
        Source source = from.source();
        if (source == null || source.detailedSources() == null) return;
        Set<Info> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        source.detailedSources().forEachReference((target, s) -> targets.add(target));
        for (Info to : targets) {
            switch (to) {
                case MethodInfo mi -> {
                    if (!hasReferenceEdge(from, mi)) handleMethodCall(from, mi);
                }
                case FieldInfo fi -> {
                    if (!accept(fi.owner())) continue;
                    boolean inverted = from instanceof MethodInfo m && m.typeInfo() == fi.owner();
                    Info edgeFrom = inverted ? fi : from;
                    Info edgeTo = inverted ? from : fi;
                    if (!hasReferenceEdge(edgeFrom, edgeTo)) builder.mergeEdge(edgeFrom, edgeTo, REFERENCES);
                }
                case TypeInfo ti -> {
                    // ⛔ "already has an edge" means a DECLARATION or HIERARCHY edge, NOT a doc edge. doJavadoc runs
                    // before this, so a member that DOCUMENTS [T] -- `{@link T}`, `[T]` -- and then uses T where the
                    // desugared CST keeps no element (inside a lambda passed to a library function) had its doc edge
                    // read as "already there" and lost its reference edge entirely: it named T, and "who refers to T"
                    // could not say so. The threshold is what the comment above always meant.
                    // TestDocLinkDoesNotSuppressARecordedReference (maddi-run-kotlin).
                    Map<Info, Long> edges = builder.edges(from);
                    Long weight = edges == null ? null : edges.get(ti);
                    if (weight == null || weight < REFERENCES) addType(from, ti.asSimpleParameterizedType(), REFERENCES);
                }
                default -> {
                }
            }
        }
    }

    /* ---------------------------------------------------------------------------------------------------------
    BY-NAME REFERENCES: a string literal that a declared sink turns into a type. See ByNameSink for why the sinks
    are declared rather than inferred, and ByNameReference for what a row carries.

    The edge is BY_NAME_REFERENCES, a SOFT lane below isAtLeastReference's threshold, so the type graph, the
    cycles, the giant and the analysis order are all untouched: this is not a compile-time dependency, and a
    campaign that priced one would be pricing something javac cannot see. A consumer that wants these -- a dead-code
    pass that must not delete a type only a string reaches, a rename that must edit the literal -- asks for them.

    ⚠ Ordering: this runs from the Visitor, i.e. BEFORE doRecordedReferences for the same member. That is safe
    only because the type arm there now tests `< REFERENCES` rather than "any edge": with the old test, a by-name
    edge to T would have suppressed a Kotlin front-end record of a real reference to T. The two were fixed in that
    order for that reason.
     --------------------------------------------------------------------------------------------------------- */

    private void doByNameSinks(Info from, MethodInfo called, List<Expression> arguments) {
        if (byNameSinks.isEmpty() || called == null) return;
        TypeInfo declaring = called.typeInfo();
        if (declaring == null) return;
        String declaringFqn = declaring.fullyQualifiedName();
        String declaringSimple = declaring.simpleName();
        for (ByNameSink sink : byNameSinks) {
            if (!sink.matches(declaringFqn, declaringSimple, called.name(), called.parameters().size())) continue;
            if (sink.classArgument() >= arguments.size()) continue; // a varargs call with fewer arguments written
            Literal name = literalOf(arguments.get(sink.classArgument()));
            if (name == null) {
                // the sink WAS called; we simply cannot read the name. Counted, never guessed at.
                ++unresolvedSinkCalls;
                continue;
            }
            Literal member = sink.memberArgument() >= 0 && sink.memberArgument() < arguments.size()
                    ? literalOf(arguments.get(sink.memberArgument())) : null;
            List<TypeInfo> targets = byNameParseResult.typeByBinaryName(name.value);
            if (targets.isEmpty()) {
                // The name is readable and names nothing here: a JDK class, a type in another project, a typo --
                // or a binding whose target was just deleted or renamed. RECORDED, not skipped: a broken binding
                // does not produce an invalid row, it produces NO row, and nothing downstream can detect an
                // absence. This is that absence made positive. Judging which of these are defects is the
                // reader's job, not this producer's.
                byNameDanglings.add(new ByNameDangling(from, sink, name.value, name.source,
                        name.owner == null ? from : name.owner, name.viaConstant,
                        member == null ? null : member.value));
                continue;
            }
            for (TypeInfo target : targets) {
                Info targetMember = member == null ? null : uniqueMemberNamed(target, member.value);
                byNameReferences.add(new ByNameReference(from, sink, target, name.value, name.source,
                        name.owner == null ? from : name.owner, name.viaConstant,
                        member == null ? null : member.value,
                        member == null ? null : member.source, targetMember));
                // the ROW is recorded whatever accept() says -- it is a fact about the source text -- but the EDGE
                // obeys the same filter as every other producer, and the same self-link rule
                if (accept(target) && target != from && !from.typeInfo().isEnclosedIn(target)) {
                    builder.mergeEdge(from, target, BY_NAME_REFERENCES);
                }
                // ⭐ AND AN EDGE TO THE MEMBER, when the name resolved to exactly one. The type edge alone is not
                // enough for a reader that works member by member: a dead-code pass keeping `MutationVerbHandler`
                // alive while deleting the `instance` field the binding reads has broken the binding just as
                // thoroughly. Consumers that only want types filter on the vertex, which is cheap; a consumer that
                // needed the member and did not have it has no way to recover it from the graph.
                if (targetMember != null && targetMember != from && accept(target)) {
                    builder.mergeEdge(from, targetMember, BY_NAME_REFERENCES);
                }
            }
        }
    }

    /** A string literal read from an argument, and where it is actually written. */
    /**
     * @param owner the member whose source text holds {@code source}: null when the literal is written at the
     *              call (the caller substitutes {@code from}), the FIELD when it came through a constant. A
     *              {@link Source} has a line and a position but no file, so without this a rewriting verb cannot
     *              tell which file to open — and the constant can sit in a different TYPE from the caller.
     */
    private record Literal(String value, Source source, boolean viaConstant, Info owner) {
    }

    /**
     * The literal behind an argument: written there, or held by a {@code static final} field it names.
     * <p>
     * ⛔ <b>Exactly one hop, and no further.</b> maddi has no constant evaluation (there is no {@code constantValue}
     * or {@code isCompileTimeConstant} anywhere), so this is the hand-rolled pattern half a dozen callers already
     * use — {@code fieldInfo.initializer() instanceof StringConstant}. One hop covers the shape that matters, a
     * {@code private static final String TARGET = "a.b.X"} beside the call; anything deeper is flow analysis, and
     * a sink whose name travels that far should be DECLARED at the point where the string is written instead.
     */
    private static Literal literalOf(Expression expression) {
        if (expression instanceof StringConstant sc) {
            return new Literal(sc.constant(), sc.source(), false, null);
        }
        if (expression instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            FieldInfo fieldInfo = fr.fieldInfo();
            if (fieldInfo.isStatic() && fieldInfo.isFinal()
                && fieldInfo.initializer() instanceof StringConstant sc) {
                return new Literal(sc.constant(), sc.source(), true, fieldInfo);
            }
        }
        return null;
    }

    /**
     * The one member of {@code target} called {@code name}, or null when there is none or several.
     * <p>
     * ⚠ Deliberately crude, and the ambiguity is deliberately left unresolved. Which overload a binding picks is
     * the SINK's rule — by arity, by an exact {@code MethodType}, "the only public static one" — and a graph
     * producer that guessed would be writing an invariant nothing maintains. A reader that needs the answer has
     * the row, the target and the name, and can apply the rule it actually implements.
     */
    private static Info uniqueMemberNamed(TypeInfo target, String name) {
        FieldInfo field = target.getFieldByName(name, false);
        if (field != null) return field;
        List<MethodInfo> methods = target.methods().stream().filter(m -> m.name().equals(name)).toList();
        return methods.size() == 1 ? methods.getFirst() : null;
    }

    private boolean hasReferenceEdge(Info from, Info to) {
        Map<Info, Long> edges = builder.edges(from);
        Long weight = edges == null ? null : edges.get(to);
        return weight != null && isReference(weight);
    }

    private void go(ModuleInfo moduleInfo) {
        builder.addVertex(moduleInfo);
        for (ModuleInfo.Uses uses : moduleInfo.uses()) {
            TypeInfo api = uses.apiResolved();
            if (api != null) {
                builder.mergeEdge(moduleInfo, api, REFERENCES);
            }
        }
        for (ModuleInfo.Provides provides : moduleInfo.provides()) {
            TypeInfo api = provides.apiResolved();
            if (api != null) {
                builder.mergeEdge(moduleInfo, api, REFERENCES);
            }
            for (TypeInfo implementation : provides.implementationsResolved()) {
                if (implementation != null) {
                    builder.mergeEdge(moduleInfo, implementation, REFERENCES);
                }
            }
        }
    }

    /*
    ⛔ A LINK IS AN EDGE, SO IT OBEYS THE SAME TWO RULES AS EVERY OTHER EDGE PRODUCER. It used to merge an edge for
    any resolved tag whatsoever, which is neither of them:

    - accept(): `{@link java.util.List}` in a comment put a VERTEX for an out-of-parse type into a graph every
      other producer keeps closed (addType, doAnnotations, handleMethodCall all filter). The doc edge itself is
      below every consumer's threshold, but the vertex is not -- a walk over vertices() saw a type nothing in the
      parse declares. Measured: one class comment, one java.util.List vertex.
    - the self-link: `{@link X}` inside X. addType refuses a member naming its own type, and states why ("says
      nothing about what must exist first"); a comment saying it is no different.

    A tag resolving to a MEMBER is filtered by its owner, for the same reason handleMethodCall filters by
    to.typeInfo(): the member of an accepted type is accepted.
     */
    private void doJavadoc(Info from) {
        if (from.javaDoc() != null) {
            for (JavaDoc.Tag tag : from.javaDoc().tags()) {
                if (tag.resolvedReference() instanceof Info to) {
                    TypeInfo owner = to instanceof TypeInfo ti ? ti : to.typeInfo();
                    if (owner == null || !accept(owner)) continue;
                    if (to == from || from.typeInfo() != null && from.typeInfo().isEnclosedIn(owner) && to == owner) {
                        continue; // a self-link
                    }
                    builder.mergeEdge(from, to, DOC_REFERENCES);
                }
            }
        }
    }

    private void doAnnotations(Info from, long weight) {
        // references to classes: use accept(), not externalsToAccept, so that an annotation whose type is a
        // source (internal) type -- e.g. a project-defined @Inject -- is recorded like any other declaration
        // reference. externalsToAccept only decides which EXTERNAL types to keep; internal types are covered by
        // the primaryTypes check inside accept(). Using externalsToAccept here silently dropped every internal
        // annotation edge (addType, for ordinary references, correctly uses accept()).
        from.annotations().stream()
                .map(AnnotationExpression::typeInfo)
                .filter(this::accept)
                .forEach(to -> builder.mergeEdge(from, to, weight));
        // references to fields
        from.annotations().stream()
                .flatMap(ae -> ae.keyValuePairs().stream().map(AnnotationExpression.KV::value))
                .forEach(e -> {
                    Visitor visitor = new Visitor(from);
                    e.visit(visitor);
                });
    }

    class Visitor implements Predicate<Element> {
        private final Info info;

        Visitor(Info info) {
            this.info = info;
        }

        public boolean test(Element e) {
            if (!e.annotations().isEmpty()) doAnnotations(info, REFERENCES);
            if (e instanceof VariableExpression ve
                && ve.variable() instanceof FieldReference fr
                && accept(fr.fieldInfo().owner())) {
                // inside a type, an accessor should come before its field
                // outside a type, we want the field to have been processed first
                // see e.g. TestStaticValuesRecord,2
                handleFieldAccess(info, fr);
            }
            if (e instanceof Assignment a
                && a.variableTarget() instanceof FieldReference fr
                && accept(fr.fieldInfo().owner().primaryType())) {
                handleFieldAccess(info, fr);
            }
            if (e instanceof LocalVariableCreation lvc) {
                addType(info, lvc.localVariable().parameterizedType(), REFERENCES);
                return true; // into assignment expression(s)
            }
            if (e instanceof MethodCall mc) {
                handleMethodCall(info, mc.methodInfo());
                mc.typeArguments().forEach(pt -> addType(info, pt, REFERENCES));
                doByNameSinks(info, mc.methodInfo(), mc.parameterExpressions());
                return true;
            }
            if (e instanceof MethodReference mr) {
                handleMethodCall(info, mr.methodInfo());
                return true;
            }
            if (e instanceof ExplicitConstructorInvocation eci &&
                // cover Object, Enum, Record, Annotation, etc.
                !eci.methodInfo().typeInfo().packageName().startsWith("java.lang")) {
                builder.mergeEdge(info, eci.methodInfo(), CODE_STRUCTURE); // S
                // ⛔ AND ALSO D: `this(..)` / `super(..)` IS A CALL, and for a long time only the S bit said so.
                // The class comment defines D as "from method body to any method referenced (as method call,
                // constructor call, method reference)", and an explicit constructor invocation is exactly that
                // -- but it was recorded as CODE STRUCTURE alone, which is what the analysis ORDER needs and
                // not what the edge IS. Every consumer that asks "is this a genuine call" with isReference()
                // (rather than isAtLeastReference(), which admits structure) therefore could not see it: gap
                // #124 measured fieldsMethodsThatCall(C(String,int)) returning THE EMPTY SET for a sibling
                // `this("n", n)`, while the parse modelled the invocation with a methodInfo() and a source
                // range. A lever trusting that answer deletes the constructor, leaves the sibling naming it,
                // and reports "1 call site rewritten" while two existed.
                // ▶ The S edge is kept exactly as it was, for BOTH kinds: the ordering argument for it has not
                // changed. handleMethodCall adds the R bit under the SAME accept()/recursion rules as every
                // other call, so an invocation of an out-of-parse supertype constructor stays structure-only.
                // ⛔⛔ ONLY WHAT THE AUTHOR WROTE IS A CALL. An IMPLICIT super() is synthesised for every
                // constructor that does not write one, so counting synthetic invocations as references gives
                // EVERY subclass a use-edge to its parent -- measured on the 500-type clustering stress model
                // (TestTypesIntoPackagesStress): 182 type-use edges that no planted call justifies, and 334
                // weights moved with them, because a callee's weight is diluted by its number of users. The
                // hierarchy is already an edge (B) and the implementation graph already models it; a written
                // this(..)/super(..) is a call site a lever can rewrite, and a synthesised one is not.
                if (!eci.isSynthetic()) handleMethodCall(info, eci.methodInfo()); // D
            }
            if (e instanceof ConstructorCall cc) {
                TypeInfo anonymousType = cc.anonymousClass();
                cc.typeArguments().forEach(pt -> addType(info, pt, REFERENCES));
                // new ArrayList<X>, we must refer to X
                addType(info, cc.parameterizedType(), REFERENCES);

                // important: check anonymous type first, it can have constructor != null
                if (anonymousType != null) {
                    handleTypeDeclaredInBody(info, anonymousType); // A
                    for (MethodInfo mi : anonymousType.constructorsAndMethods()) {
                        handleMethodCall(info, mi);
                    }
                    // the arguments to 'new X(...) { }' (and its scope) are NOT part of the anonymous body:
                    // visit them explicitly, otherwise a type referenced only there -- e.g. 'new Y()' passed as
                    // an argument, or 'Y::new' in an enum constant that has a body -- is dropped when we stop
                    // descending here.
                    if (cc.object() != null) cc.object().visit(this);
                    cc.parameterExpressions().forEach(arg -> arg.visit(this));
                    return false;
                }
                if (cc.constructor() != null) {
                    handleMethodCall(info, cc.constructor());
                    // a corpus's own container is often a CONSTRUCTOR taking the name (Cassandra's
                    // ParameterizedClass), so a sink may name one; the declaring type and arity match as they do
                    // for a method, and a constructor's name is its simple name
                    doByNameSinks(info, cc.constructor(), cc.parameterExpressions());
                }
                return true;
            }
            if (e instanceof Lambda lambda) {
                TypeInfo anonymousType = lambda.methodInfo().typeInfo();
                handleTypeDeclaredInBody(info, anonymousType);
                //handleMethodCall(info, lambda.methodInfo()); is this needed?
                return false;
            }
            // ⛔ A LOCAL CLASS IS THE THIRD KIND OF TYPE DECLARED INSIDE A BODY, and it was the one kind this
            // visitor did not know about. Edge type A in the class comment has always promised "from a method or
            // field into its anonymous, lambda, LOCAL types"; the anonymous and lambda arms are just above, and
            // this one was missing -- because both of those arrive as EXPRESSIONS (ConstructorCall.anonymousClass,
            // Lambda) while a local class arrives as a STATEMENT, and the visitor's chain tests expressions.
            //
            // ⚠ WHAT WAS ACTUALLY WRONG, measured rather than read (the gap record's account of the mechanism
            // differs): the local type had NO structural edge at all. It appeared in the graph only as the TARGET
            // of the enclosing method's reference edges -- `go() -R-> Helper`, from addType on `new Helper()`,
            // plus -R-> its constructor and the method called on it -- so it was a vertex with no outgoing edges
            // and nothing declaring it. go(Helper) was never called, so neither its members' CODE_STRUCTURE edges
            // nor anything its body references existed. An anonymous class in the identical shape yields
            // `$0.run() -R-> a.Made`; the local class yielded nothing.
            //
            // Every consumer of the type graph was short by that: requiredTypes, buildUnitDependencies,
            // cyclicTypeComponents, the package graph. GAP G15 (OpenSearch campaign), pinned from the query side
            // by TestGap9LambdaWrittenRequirement.
            //
            // Same treatment as the other two arms, and for the same reason: go() walks the declared type's own
            // members and their bodies, so we must NOT also descend here -- that would attribute the body's
            // references to the enclosing member as well and double every edge weight. LocalTypeDeclarationImpl
            // does not descend either ("following anonymous class, we're not going deeper"), so the return value
            // is belt and braces.
            // ⚠ The declared type is NOT among the enclosing type's subTypes() -- that is why go(TypeInfo) never
            // reached it -- so this is the only walk it gets, and it gets it exactly once. TestCallGraphLocalClass
            // pins that: a second walk would double the weights, not merely duplicate a line.
            if (e instanceof LocalTypeDeclaration ltd) {
                handleTypeDeclaredInBody(info, ltd.typeInfo()); // A
                return false;
            }
            if (e instanceof TypeExpression te) {
                if (!info.typeInfo().isEnclosedIn(te.parameterizedType().typeInfo())) {
                    addType(info, te.parameterizedType(), REFERENCES);
                } // else: recursion in lambdas
            }
            if (e instanceof ClassExpression ce) {
                addType(info, ce.type(), REFERENCES);
            }
            if (e instanceof InstanceOf io) {
                addType(info, io.testType(), REFERENCES);
            }
            if (e instanceof Cast cast) {
                addType(info, cast.parameterizedType(), REFERENCES);
            }
            if (e instanceof TryStatement.CatchClause catchClause) {
                catchClause.exceptionTypes().forEach(et -> addType(info, et, REFERENCES));
            }
            if (e instanceof RecordPattern rp) {
                if (rp.localVariable() != null) {
                    addType(info, rp.localVariable().parameterizedType(), REFERENCES);
                } else if (rp.recordType() != null) {
                    addType(info, rp.recordType(), REFERENCES);
                }
            }
            return true;
        }
    }

    /**
     * <b>Warning: this edge is inverted for a method's own fields, unlike every other reference edge.</b>
     * <p>
     * When {@code info} is a method of the very type that owns the field, the edge runs
     * {@code field -> method}; in every other case it runs {@code info -> field}. The inversion is
     * deliberate and load-bearing — the analysis order has to reach a field's value before the methods
     * that consume it, so the arrow means "this value flows into that method", not "that method depends
     * on this field". Reversing it would put every accessor ahead of the state it reads.
     * <p>
     * The cost is borne by readers: <b>walking only a member's outgoing edges misses every access it
     * makes to its own type's state</b>, silently, since nothing is absent from the graph — the relation
     * merely points the other way. Anything asking "what does this member touch" must also collect the
     * incoming edges from {@code FieldInfo} vertices owned by that member's type. See edge type E in the
     * class comment.
     */
    private void handleFieldAccess(Info info, FieldReference fr) {
        if (info instanceof MethodInfo mi && mi.typeInfo() == fr.fieldInfo().owner()) {
            builder.mergeEdge(fr.fieldInfo(), info, REFERENCES); // INVERTED on purpose; see javadoc
        } else {
            builder.mergeEdge(info, fr.fieldInfo(), REFERENCES);
        }
    }

    /**
     * Edge type A for a type <em>declared inside a body</em>: an anonymous class, a lambda, or a local class.
     * The structural edge says "this member declares that type", and {@link #go(TypeInfo)} then walks the declared
     * type exactly as it walks a named one — its members, their bodies, everything they reference.
     * <p>
     * ⚠ Named for what the three have in common rather than for the anonymous case alone, which is what it used to
     * be called: a <b>local class is not anonymous</b> (it has a simple name, and the pin test asserts
     * {@code anon=false}), and while this method carried the narrower name the local-class arm simply did not
     * exist. See the {@code LocalTypeDeclaration} branch in {@link Visitor#test}.
     * <p>
     * Called exactly once per declared type: none of the three is among its enclosing type's {@code subTypes()},
     * so {@code go(TypeInfo)}'s subtype loop does not reach them. A second call would not duplicate an edge — the
     * builder sums weights — it would double the reference counts.
     */
    private void handleTypeDeclaredInBody(Info from, TypeInfo declaredType) {
        builder.mergeEdge(from, declaredType, CODE_STRUCTURE);
        go(declaredType);
    }

    private void handleMethodCall(Info from, MethodInfo to) {
        if (from == to) {
            recursive.add(to);
        } else if (from instanceof MethodInfo mi && isRecursion(mi, to)) {
            recursive.add(mi);
            recursive.add(to);
        } else if (accept(to.typeInfo())) {
            builder.mergeEdge(from, to, REFERENCES);
        }
    }

    private static boolean isRecursion(MethodInfo from, MethodInfo to) {
        if (from == to) return true;
        TypeInfo owner = from.typeInfo();
        if (owner.enclosingMethod() != null) {
            return isRecursion(owner.enclosingMethod(), to);
        }
        return false;
    }

    /*
    The skip is a SELF-link check, and nothing wider: a member naming its own type -- or a type that encloses
    it -- says nothing about what must exist first.

    It used to be written as "is the referenced type assignable to the referring one", which also dropped every
    reference from a supertype to its own SUBTYPE. That is a real compile dependency, and precisely the edge a
    cycle or seam analysis is looking for: an abstraction naming its implementation. Elasticsearch,
    AggregatorBase:137 -- `parent instanceof RandomSamplerAggregator == false`, where RandomSamplerAggregator
    is (transitively) an AggregatorBase -- was invisible to path.seamVerdicts, which therefore called a seam
    clean and let the carve break :server. The sealed-permits edge (Shape->H->Circle) was already recorded, so
    the graph was inconsistent about the same direction.
     */
    private void addType(Info from, ParameterizedType pt, long edgeValue) {
        TypeInfo best = pt.bestTypeInfo();
        if (best != null) {
            if (best != from && !from.typeInfo().isEnclosedIn(best) && accept(best)) {
                builder.mergeEdge(from, best, edgeValue);
            }
            for (ParameterizedType parameter : pt.parameters()) {
                addType(from, parameter, TYPES_IN_DECLARATION);
            }
        }
    }

    private boolean accept(TypeInfo typeInfo) {
        return primaryTypes.contains(typeInfo.primaryType()) || externalsToAccept.test(typeInfo);
    }
}
