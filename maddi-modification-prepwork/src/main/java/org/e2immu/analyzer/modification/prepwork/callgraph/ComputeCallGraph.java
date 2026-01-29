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
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.element.ElementImpl;
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
            mi.methodBody().visit(visitor); // D
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
        // references to classes
        from.annotations().stream()
                .map(AnnotationExpression::typeInfo)
                .filter(externalsToAccept)
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

    private void handleFieldAccess(Info info, FieldReference fr) {
        if (info instanceof MethodInfo mi && mi.typeInfo() == fr.fieldInfo().owner()) {
            builder.mergeEdge(fr.fieldInfo(), info, REFERENCES);
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
        if (!from.typeInfo().asParameterizedType().isAssignableFrom(runtime, pt)) {
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
