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

package org.e2immu.analyzer.modification.prepwork.callgraph;


import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.ExplicitConstructorInvocation;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.e2immu.language.cst.impl.analysis.ValueImpl.BoolImpl.TRUE;

/*
call & reference graphs.

direction of arrow: I need you to exist first (I, from -> you, to)
 */
public class ComputeCallGraph {
    public static final Property RECURSIVE_METHOD = new PropertyImpl("recursiveMethod", ValueImpl.BoolImpl.FALSE);
    private final Runtime runtime;
    private final Set<TypeInfo> primaryTypes;
    private final Set<MethodInfo> recursive = new HashSet<>();
    private final G.Builder<Info> builder = new ImmutableGraph.Builder<>(Long::sum);
    private final Predicate<TypeInfo> externalsToAccept;
    private final Collection<ModuleInfo> moduleInfos;

    private static final long CODE_STRUCTURE_BITS = 48;
    public static final long CODE_STRUCTURE = 1L << CODE_STRUCTURE_BITS;
    private static final long TYPE_HIERARCHY_BITS = 40;
    public static final long TYPE_HIERARCHY = 1L << TYPE_HIERARCHY_BITS;
    private static final long TYPES_IN_DECLARATION_BITS = 32;
    public static final long TYPES_IN_DECLARATION = 1L << TYPES_IN_DECLARATION_BITS;
    private static final long REFERENCES_BITS = 16;
    public static final long REFERENCES = 1L << REFERENCES_BITS;
    private static final long DOC_REFERENCES = 1;

    private G<Info> graph;

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

    public static String print(G<Info> graph) {
        return graph.toString(", ", ComputeCallGraph::edgeValuePrinter);
    }

    public static String edgeValuePrinter(long value) {
        StringBuilder sb = new StringBuilder();
        if (value >= CODE_STRUCTURE) sb.append("S");
        if ((value & (CODE_STRUCTURE - 1)) >= TYPE_HIERARCHY) sb.append("H");
        if ((value & (TYPE_HIERARCHY - 1)) >= TYPES_IN_DECLARATION) sb.append("D");
        if ((value & (TYPES_IN_DECLARATION - 1)) >= REFERENCES) sb.append("R");
        if ((value & (REFERENCES - 1)) >= 1) sb.append("d");
        return sb.toString();
    }

    public static int weightedSumInteractions(long l, int docsWeight, int refsWeight, int declarationWeight,
                                              int hierarchyWeight, int codeStructureWeight) {
        int docs = (int) (l & (REFERENCES));
        int refs = (int) ((l & (TYPES_IN_DECLARATION - 1)) >> REFERENCES_BITS);
        int declaration = (int) ((l & (TYPE_HIERARCHY - 1)) >> TYPES_IN_DECLARATION_BITS);
        int hierarchy = (int) ((l & (CODE_STRUCTURE - 1)) >> TYPE_HIERARCHY_BITS);
        int codeStructure = (int) (l >> CODE_STRUCTURE_BITS);
        return docs * docsWeight + refs * refsWeight + declaration * declarationWeight + hierarchy * hierarchyWeight
               + codeStructure * codeStructureWeight;
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
        });
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
            TypeInfo implementation = provides.implementationResolved();
            if (implementation != null) {
                builder.mergeEdge(moduleInfo, implementation, REFERENCES);
            }
        }
    }

    private void doJavadoc(Info from) {
        if (from.javaDoc() != null) {
            for (JavaDoc.Tag tag : from.javaDoc().tags()) {
                if (tag.resolvedReference() instanceof Info to) {
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
            }
            if (e instanceof ConstructorCall cc) {
                TypeInfo anonymousType = cc.anonymousClass();
                cc.typeArguments().forEach(pt -> addType(info, pt, REFERENCES));
                // new ArrayList<X>, we must refer to X
                addType(info, cc.parameterizedType(), REFERENCES);

                // important: check anonymous type first, it can have constructor != null
                if (anonymousType != null) {
                    handleAnonymousType(info, anonymousType); // B
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
                }
                return true;
            }
            if (e instanceof Lambda lambda) {
                TypeInfo anonymousType = lambda.methodInfo().typeInfo();
                handleAnonymousType(info, anonymousType);
                //handleMethodCall(info, lambda.methodInfo()); is this needed?
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

    private void handleAnonymousType(Info from, TypeInfo anonymousType) {
        builder.mergeEdge(from, anonymousType, CODE_STRUCTURE);
        go(anonymousType);
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

    private void addType(Info from, ParameterizedType pt, long edgeValue) {
        if (!runtime.isAssignableFrom(from.typeInfo().asParameterizedType(), pt)) {
            TypeInfo best = pt.bestTypeInfo();
            if (best != null) {
                if (best != from && accept(best)) {
                    builder.mergeEdge(from, best, edgeValue);
                }
                for (ParameterizedType parameter : pt.parameters()) {
                    addType(from, parameter, TYPES_IN_DECLARATION);
                }
            }
        } // else: avoid links to self, we want the type at the end
    }

    private boolean accept(TypeInfo typeInfo) {
        return primaryTypes.contains(typeInfo.primaryType()) || externalsToAccept.test(typeInfo);
    }
}
