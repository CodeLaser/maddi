/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.run.rewire;

import io.codelaser.maddi.callgraph.ComputeCallGraph;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Is an incremental re-parse (and the prep after it) what a cold one would have produced? The oracle of
 * {@code docs/design/incremental-and-overlay.md} §8. Parsing and prep are deterministic, so the answer is exact:
 * anything that differs from a cold parse of the same files, in a fresh inspector, is a defect of the incremental
 * path.
 * <p>
 * Five checks, each blind to something the others see:
 * <ol>
 *     <li>{@link #staleReferences}: no reference anywhere in the result reaches an object that is not the live one
 *     for its type. A stale pointer is what a wrong rewire leaves behind, and no comparison by name or by equality
 *     sees it, both being fully qualified name plus source set.</li>
 *     <li>{@link #printed}: every compilation unit prints as the cold parse prints it.</li>
 *     <li>{@link #variablesRead}: every method body and field initializer reads the same variables, with the same
 *     field scopes written. The Java printer prints {@code v} and {@code this.v} alike; the codec and the Kotlin
 *     printer do not, and a rewire once turned the one into the other.</li>
 *     <li>{@link #callGraphEdges}: after prep, the call graph has the same edges with the same weights.</li>
 * </ol>
 */
public final class ReparseOracle {

    private ReparseOracle() {
    }

    /**
     * Every difference between an incremental result and a cold one, as readable lines; empty when there is none.
     * The call graphs are compared only when both are given.
     */
    public static List<String> differences(JavaInspector warm, ParseResult warmResult, G<Info> warmGraph,
                                           JavaInspector cold, ParseResult coldResult, G<Info> coldGraph) {
        List<String> differences = new ArrayList<>();
        staleReferences(warmResult).forEach(s -> differences.add("stale reference: " + s));
        compare("printed", printed(cold, coldResult), printed(warm, warmResult), differences);
        compare("variables read", variablesRead(coldResult), variablesRead(warmResult), differences);
        compare("declaration", declarations(coldResult), declarations(warmResult), differences);
        if (warmGraph != null && coldGraph != null) {
            compare("call graph", Map.of("edges", callGraphEdges(coldGraph)), Map.of("edges", callGraphEdges(warmGraph)),
                    differences);
        }
        return differences;
    }

    private static <T> void compare(String what, Map<String, T> cold, Map<String, T> warm, List<String> differences) {
        if (!cold.keySet().equals(warm.keySet())) {
            Set<String> onlyCold = cold.keySet().stream().filter(k -> !warm.containsKey(k))
                    .collect(Collectors.toCollection(java.util.TreeSet::new));
            Set<String> onlyWarm = warm.keySet().stream().filter(k -> !cold.containsKey(k))
                    .collect(Collectors.toCollection(java.util.TreeSet::new));
            differences.add(what + ": only in the cold parse " + onlyCold + ", only in the incremental one " + onlyWarm);
        }
        for (Map.Entry<String, T> e : cold.entrySet()) {
            T w = warm.get(e.getKey());
            if (w != null && !Objects.equals(e.getValue(), w)) {
                differences.add(what + " of " + e.getKey() + " differ:\n  cold: " + e.getValue() + "\n  incremental: "
                                + w);
            }
        }
    }

    /**
     * The references, anywhere in {@code parseResult}, to one of its own source types that do not reach the object
     * the parse result holds for that type: {@code "holder -> reached"}. Walks the type references of every primary
     * type, its hierarchy, the signatures of its methods and fields, and in every method body and field initializer
     * the owners of the methods called, the constructors invoked and the fields read.
     */
    /*
     Where a type reference sits, for a stale one that no structural check above names: the same parts
     TypeInfo.typesReferenced concatenates, asked one by one. Only computed for a reference found stale.
     */
    private static String whereReferenced(TypeInfo primaryType, TypeInfo reached) {
        java.util.function.Predicate<java.util.stream.Stream<io.codelaser.maddi.cst.api.element.Element.TypeReference>>
                has = stream -> stream.anyMatch(tr -> tr.typeInfo() == reached);
        List<String> where = new ArrayList<>();
        primaryType.recursiveSubTypeStream().forEach(type -> {
            String t = type.fullyQualifiedName();
            if (type.permittedWhenSealed().contains(reached)) where.add("permits of " + t);
            if (has.test(type.annotations().stream().flatMap(a -> a.typesReferenced(_ -> true)))) {
                where.add("annotations of " + t);
            }
            if (type.javaDoc() != null && has.test(type.javaDoc().typesReferenced(_ -> true))) {
                where.add("javadoc of " + t);
            }
            if (has.test(type.typeParameters().stream().flatMap(tp -> tp.typesReferenced(_ -> true)))) {
                where.add("type parameters of " + t);
            }
            type.constructorAndMethodStream().forEach(mi -> {
                String m = mi.fullyQualifiedName();
                if (!has.test(mi.typesReferenced(_ -> true))) return;
                List<String> parts = new ArrayList<>();
                if (mi.methodBody() != null && has.test(mi.methodBody().typesReferenced(_ -> true))) parts.add("body");
                if (has.test(mi.annotations().stream().flatMap(a -> a.typesReferenced(_ -> true)))) {
                    parts.add("annotations");
                }
                if (mi.javaDoc() != null && has.test(mi.javaDoc().typesReferenced(_ -> true))) parts.add("javadoc");
                if (has.test(mi.typeParameters().stream().flatMap(tp -> tp.typesReferenced(_ -> true)))) {
                    parts.add("type parameters");
                }
                if (has.test(mi.parameters().stream().flatMap(pi -> pi.typesReferenced(_ -> true)))) {
                    parts.add("parameters");
                }
                if (has.test(mi.exceptionTypes().stream().flatMap(et -> et.typesReferenced(
                        io.codelaser.maddi.cst.api.element.Element.TypeReferenceNature.EXPLICIT, null)))) {
                    parts.add("exception types");
                }
                where.add(m + " " + (parts.isEmpty() ? "elsewhere" : parts));
            });
            for (FieldInfo fieldInfo : type.fields()) {
                if (!has.test(fieldInfo.typesReferenced(_ -> true))) continue;
                List<String> parts = new ArrayList<>();
                if (fieldInfo.initializer() != null && has.test(fieldInfo.initializer().typesReferenced(_ -> true))) {
                    parts.add("initializer");
                }
                if (has.test(fieldInfo.annotations().stream().flatMap(a -> a.typesReferenced(_ -> true)))) {
                    parts.add("annotations");
                }
                if (fieldInfo.javaDoc() != null && has.test(fieldInfo.javaDoc().typesReferenced(_ -> true))) {
                    parts.add("javadoc");
                }
                where.add(fieldInfo.fullyQualifiedName() + " " + (parts.isEmpty() ? "elsewhere" : parts));
            }
        });
        return where.isEmpty() ? "" : " in " + String.join("; ", where);
    }

    public static List<String> staleReferences(ParseResult parseResult) {
        List<String> stale = new ArrayList<>();
        Set<String> ours = parseResult.primaryTypes().stream().map(TypeInfo::fullyQualifiedName)
                .collect(Collectors.toUnmodifiableSet());
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            java.util.function.BiConsumer<TypeInfo, String> check = (reached, where) -> {
                if (reached == null) return;
                TypeInfo primary = reached.primaryType();
                if (primary != null && ours.contains(primary.fullyQualifiedName())
                    && parseResult.findType(primary.fullyQualifiedName()) != primary) {
                    stale.add(primaryType.fullyQualifiedName() + " -> " + reached.fullyQualifiedName() + " (" + where
                              + ")");
                }
            };
            primaryType.typesReferenced(_ -> true).forEach(tr -> check.accept(tr.typeInfo(),
                    "a type reference" + whereReferenced(primaryType, tr.typeInfo())));
            primaryType.recursiveSubTypeStream().forEach(type -> {
                String t = type.fullyQualifiedName();
                if (type.parentClass() != null) check.accept(type.parentClass().typeInfo(), "parent of " + t);
                type.interfacesImplemented().forEach(pt -> check.accept(pt.typeInfo(), "interface of " + t));
                type.constructorAndMethodStream().forEach(mi -> {
                    String m = mi.fullyQualifiedName();
                    check.accept(mi.typeInfo(), "owner of " + m);
                    if (mi.returnType() != null) check.accept(mi.returnType().typeInfo(), "return type of " + m);
                    mi.parameters().forEach(pi -> check.accept(pi.parameterizedType().typeInfo(), "parameter of " + m));
                    mi.methodBody().visit(e -> reached(e, r -> check.accept(r, e.getClass().getSimpleName() + " in "
                                                                              + m)));
                });
                for (FieldInfo fieldInfo : type.fields()) {
                    String f = fieldInfo.fullyQualifiedName();
                    check.accept(fieldInfo.owner(), "owner of " + f);
                    check.accept(fieldInfo.type().typeInfo(), "type of " + f);
                    if (fieldInfo.initializer() != null) {
                        fieldInfo.initializer().visit(e -> reached(e, r -> check.accept(r,
                                e.getClass().getSimpleName() + " in the initializer of " + f)));
                    }
                }
            });
        }
        return stale;
    }

    private static boolean reached(Element element, Consumer<TypeInfo> check) {
        switch (element) {
            case MethodCall mc -> check.accept(mc.methodInfo().typeInfo());
            case ConstructorCall cc when cc.constructor() != null -> check.accept(cc.constructor().typeInfo());
            case VariableExpression ve when ve.variable() instanceof FieldReference fr ->
                    check.accept(fr.fieldInfo().owner());
            default -> {
            }
        }
        return true;
    }

    /**
     * Per type, method, field and parameter: how many comments it has, its annotations, the type-use annotations
     * of its type, and its javadoc with the target of every resolved tag. Printing hides most of this: the Java
     * printer prints a type's comments, not its {@code javaDoc()} (the Kotlin printer reads the latter), and
     * prints an annotation once however often a list holds it.
     */
    public static Map<String, String> declarations(ParseResult parseResult) {
        Map<String, String> map = new TreeMap<>();
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            primaryType.recursiveSubTypeStream().forEach(type -> {
                map.put("T " + type.fullyQualifiedName(), declaration(type.comments(), type.annotations(),
                        type.javaDoc(), null));
                type.constructorAndMethodStream().forEach(mi -> {
                    map.put("M " + mi.fullyQualifiedName(), declaration(mi.comments(), mi.annotations(),
                            mi.javaDoc(), mi.returnType()));
                    mi.parameters().forEach(pi -> map.put("P " + mi.fullyQualifiedName() + "#" + pi.index(),
                            declaration(pi.comments(), pi.annotations(), null, pi.parameterizedType())));
                });
                for (FieldInfo fieldInfo : type.fields()) {
                    map.put("F " + fieldInfo.fullyQualifiedName(), declaration(fieldInfo.comments(),
                            fieldInfo.annotations(), fieldInfo.javaDoc(), fieldInfo.type()));
                }
            });
        }
        return map;
    }

    private static String declaration(List<io.codelaser.maddi.cst.api.element.Comment> comments,
                                      List<io.codelaser.maddi.cst.api.expression.AnnotationExpression> annotations,
                                      io.codelaser.maddi.cst.api.element.JavaDoc javaDoc,
                                      io.codelaser.maddi.cst.api.type.ParameterizedType type) {
        StringBuilder sb = new StringBuilder();
        sb.append(comments.size()).append(" comment(s); annotations ")
                .append(annotations.stream().map(a -> a.typeInfo().fullyQualifiedName()).toList());
        if (type != null && !type.annotations().isEmpty()) {
            sb.append("; type-use ").append(type.annotations().stream().map(a -> a.typeInfo().fullyQualifiedName())
                    .toList());
        }
        if (javaDoc == null) {
            sb.append("; no javadoc");
        } else {
            sb.append("; javadoc ").append(javaDoc.tags().stream()
                    .map(t -> t.resolvedReference() == null ? "-" : t.resolvedReference().toString()).toList());
        }
        return sb.toString();
    }

    /** Each primary type's compilation unit, printed; by fully qualified name. */
    public static Map<String, String> printed(JavaInspector inspector, ParseResult parseResult) {
        Map<String, String> map = new TreeMap<>();
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            map.put(primaryType.fullyQualifiedName(), inspector.print2(primaryType.compilationUnit()));
        }
        return map;
    }

    /**
     * Per method and per field with an initializer: the variables it reads, in visit order, by fully qualified name,
     * with {@code " (scope written)"} after a field reference whose scope is in the source.
     */
    public static Map<String, List<String>> variablesRead(ParseResult parseResult) {
        Map<String, List<String>> map = new TreeMap<>();
        for (TypeInfo primaryType : parseResult.primaryTypes()) {
            primaryType.recursiveSubTypeStream().forEach(type -> {
                type.constructorAndMethodStream().forEach(mi -> map.put(mi.fullyQualifiedName(),
                        variablesRead(mi.methodBody())));
                for (FieldInfo fieldInfo : type.fields()) {
                    if (fieldInfo.initializer() != null) {
                        map.put(fieldInfo.fullyQualifiedName(), variablesRead(fieldInfo.initializer()));
                    }
                }
            });
        }
        return map;
    }

    private static List<String> variablesRead(Element element) {
        List<String> list = new ArrayList<>();
        element.visit(e -> {
            if (e instanceof VariableExpression ve) {
                String scope = ve.variable() instanceof FieldReference fr && !fr.isDefaultScope() ? " (scope written)"
                        : "";
                list.add(ve.variable().fullyQualifiedName() + scope);
            }
            return true;
        });
        return list;
    }

    /** The edges of a call graph, {@code "from -> to weight"}, sorted: {@code G.toString} promises no order. */
    public static List<String> callGraphEdges(G<Info> graph) {
        return graph.edgeStream()
                .map(e -> name(e.from().t()) + " -> " + name(e.to().t()) + " "
                          + ComputeCallGraph.edgeValuePrinter(e.weight()))
                .sorted()
                .toList();
    }

    private static String name(Info info) {
        return info.getClass().getSimpleName().charAt(0) + ":" + info.fullyQualifiedName();
    }
}
