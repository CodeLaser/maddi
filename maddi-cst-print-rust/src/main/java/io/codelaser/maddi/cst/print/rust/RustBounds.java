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


package io.codelaser.maddi.cst.print.rust;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The trait bounds of a type parameter, from what the Java it governs does with its values: a class's {@code T} from
 * its fields and members, a method's from its signature and body. Java needs none of them (every object has
 * {@code equals}, {@code hashCode}, {@code toString}); Rust asks for each where it is used:
 * <ul>
 *     <li>{@code Eq + std::hash::Hash}: an element of a {@code HashSet}, a key of a {@code HashMap};</li>
 *     <li>{@code Ord}: an element of a {@code BTreeSet}, a key of a {@code BTreeMap}, {@code compareTo};</li>
 *     <li>{@code PartialEq}: {@code ==}, {@code equals}, {@code contains}, {@code indexOf}, {@code remove(Object)};</li>
 *     <li>{@code std::fmt::Display}: in a string ({@code +}, {@code String.valueOf}, {@code toString}, a print, an append);</li>
 *     <li>{@code Clone}: a copy of a collection of them ({@code List.copyOf}, a copy constructor, {@code addAll},
 *     {@code Arrays.copyOf}), or a type argument of a type whose impl asks for it ({@code @Immutable(hc=true)}).</li>
 * </ul>
 * Only an impl's and a method's generics carry them; a struct declaration needs none.
 */
final class RustBounds {
    private final RustFacts facts;
    private final Map<TypeParameter, Set<String>> bounds = new HashMap<>();

    RustBounds(RustFacts facts) {
        this.facts = facts;
    }

    /** {@code T: Clone + Eq + Hash}, or {@code T}. */
    String declare(TypeParameter tp) {
        Set<String> b = of(tp);
        String name = RustNames.typeParameter(tp);
        return b.isEmpty() ? name : name + ": " + String.join(" + ", b);
    }

    Set<String> of(TypeParameter tp) {
        Set<String> known = bounds.get(tp);
        if (known != null) return known;
        bounds.put(tp, Set.of()); // a type using itself: nothing more from there
        Set<String> found = new LinkedHashSet<>();
        if (tp.getOwner().isLeft()) {
            TypeInfo ti = tp.getOwner().getLeft();
            if (facts.immutableHc(ti)) found.add("Clone");
            for (FieldInfo f : ti.fields()) types(f.type(), tp, found);
            Stream.concat(ti.constructors().stream(), ti.methods().stream()).forEach(m -> method(m, tp, found));
        } else {
            method(tp.getOwner().getRight(), tp, found);
        }
        Set<String> ordered = new LinkedHashSet<>();
        for (String b : List.of("Clone", "Ord", "Eq", "std::hash::Hash", "PartialEq", "std::fmt::Display")) {
            if (found.contains(b) && !("PartialEq".equals(b) && (found.contains("Eq") || found.contains("Ord")))
                && !("Eq".equals(b) && found.contains("Ord") && !found.contains("std::hash::Hash"))) {
                ordered.add(b);
            }
        }
        bounds.put(tp, ordered);
        return ordered;
    }

    private void method(MethodInfo m, TypeParameter tp, Set<String> found) {
        m.parameters().forEach(p -> types(p.parameterizedType(), tp, found));
        types(m.returnType(), tp, found);
        if (m.methodBody() == null) return;
        m.methodBody().visit(e -> {
            element(e, tp, found);
            return true;
        });
    }

    private void element(Element e, TypeParameter tp, Set<String> found) {
        if (e instanceof Expression x && x.parameterizedType() != null) types(x.parameterizedType(), tp, found);
        switch (e) {
            case BinaryOperator bo -> {
                String op = bo.operator().name();
                // a null check compares nothing
                if (("==".equals(op) || "!=".equals(op)) && (is(bo.lhs(), tp) || is(bo.rhs(), tp))
                    && !(bo.lhs() instanceof NullConstant) && !(bo.rhs() instanceof NullConstant)) {
                    found.add("PartialEq");
                }
                if ("+".equals(op) && RustTypes.isString(bo.parameterizedType()) && (is(bo.lhs(), tp) || is(bo.rhs(), tp))) {
                    found.add("std::fmt::Display");
                }
            }
            case MethodCall mc -> call(mc, tp, found);
            case ConstructorCall cc when cc.constructor() != null && cc.parameterExpressions().size() == 1
                                         && cc.constructor().typeInfo().compilationUnit().externalLibrary()
                                         && elementIs(cc.parameterExpressions().getFirst().parameterizedType(), tp) ->
                    found.add("Clone"); // new ArrayList<>(c): a copy
            default -> { }
        }
    }

    private void call(MethodCall mc, TypeParameter tp, Set<String> found) {
        String name = mc.methodInfo().name();
        List<Expression> args = mc.parameterExpressions();
        Expression object = mc.object();
        if (object != null && is(object, tp)) {
            switch (name) {
                case "equals" -> found.add("PartialEq");
                case "toString" -> found.add("std::fmt::Display");
                case "hashCode" -> found.add("std::hash::Hash");
                case "compareTo" -> found.add("Ord");
                default -> { }
            }
        }
        boolean onElements = object != null && elementIs(object.parameterizedType(), tp);
        if (onElements && args.size() == 1 && Set.of("contains", "indexOf", "lastIndexOf", "remove").contains(name)
            && !args.getFirst().parameterizedType().isPrimitiveExcludingVoid()) {
            found.add("PartialEq");
        }
        if (onElements && "addAll".equals(name)) found.add("Clone");
        String fqn = mc.methodInfo().typeInfo().fullyQualifiedName();
        if (("java.util.List".equals(fqn) || "java.util.Set".equals(fqn) || "java.util.Arrays".equals(fqn))
            && ("copyOf".equals(name)) && !args.isEmpty() && elementIs(args.getFirst().parameterizedType(), tp)) {
            found.add("Clone");
        }
        boolean printing = "java.io.PrintStream".equals(fqn) && (name.startsWith("print"))
                           || "java.lang.String".equals(fqn) && ("valueOf".equals(name) || "format".equals(name))
                           || fqn.endsWith("StringBuilder") && ("append".equals(name) || "insert".equals(name));
        if (printing && args.stream().anyMatch(a -> is(a, tp))) found.add("std::fmt::Display");
    }

    // what a type mentioning tp asks of it: the key of a hashed or sorted collection, the argument of a type whose
    // impl bounds it
    private void types(ParameterizedType type, TypeParameter tp, Set<String> found) {
        if (type == null || type.typeInfo() == null) return;
        List<ParameterizedType> args = type.parameters();
        String mapped = RustTypes.mapped(type.typeInfo());
        if (!args.isEmpty() && isTp(args.getFirst(), tp)) {
            if ("HashSet".equals(mapped) || "HashMap".equals(mapped)) {
                found.add("Eq");
                found.add("std::hash::Hash");
            } else if ("BTreeSet".equals(mapped) || "BTreeMap".equals(mapped)) {
                found.add("Ord");
            }
        }
        TypeInfo ti = type.typeInfo();
        if (!ti.compilationUnit().externalLibrary() && ti != ownerType(tp)) {
            List<TypeParameter> theirs = ti.typeParameters();
            for (int i = 0; i < args.size() && i < theirs.size(); i++) {
                if (isTp(args.get(i), tp)) found.addAll(of(theirs.get(i)));
            }
        }
        for (ParameterizedType a : args) types(a, tp, found);
    }

    private static TypeInfo ownerType(TypeParameter tp) {
        return tp.getOwner().isLeft() ? tp.getOwner().getLeft() : null;
    }

    private static boolean is(Expression e, TypeParameter tp) {
        return e != null && isTp(e.parameterizedType(), tp);
    }

    private static boolean isTp(ParameterizedType t, TypeParameter tp) {
        return t != null && t.isTypeParameter() && t.arrays() == 0 && tp.equals(t.typeParameter());
    }

    // a collection or an array of tp
    private static boolean elementIs(ParameterizedType t, TypeParameter tp) {
        if (t == null) return false;
        if (t.arrays() > 0) return t.typeParameter() != null && tp.equals(t.typeParameter());
        return !t.parameters().isEmpty() && isTp(t.parameters().getFirst(), tp);
    }
}
