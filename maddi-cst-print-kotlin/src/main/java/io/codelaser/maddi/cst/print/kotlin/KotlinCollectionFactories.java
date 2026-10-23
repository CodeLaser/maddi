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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Java's immutable collection factories as Kotlin's (CodeLaser/maddi#141): {@code List.of(a, b)} is
 * {@code listOf(a, b)}, {@code List.of()} is {@code emptyList()}, {@code Map.of(k, v)} is {@code mapOf(k to v)}; a Set.of
 * or Map.of of two elements or more stays Java's, whose iteration order is not Kotlin's. Only
 * where the value lands in a read-only position ({@link KotlinReadOnly#boundToReadOnly}): Kotlin's factories return
 * read-only types, and in a {@code MutableList} position Java's call stays, an immutable list typed mutable as in
 * Java. This is a table of names; whether a call's result is immutable is the archive's
 * ({@code @ImmutableContainer}), not this class's.
 */
final class KotlinCollectionFactories {

    private KotlinCollectionFactories() {
    }

    /** owner -> {empty factory, factory} */
    private static final Map<String, String[]> FACTORIES = Map.of(
            "java.util.List", new String[]{"emptyList", "listOf"},
            "java.util.Set", new String[]{"emptySet", "setOf"},
            "java.util.Map", new String[]{"emptyMap", "mapOf"});

    static boolean applies(MethodCall mc) {
        return mc.methodInfo() != null && mc.methodInfo().isStatic() && "of".equals(mc.methodInfo().name())
               && FACTORIES.containsKey(mc.methodInfo().typeInfo().fullyQualifiedName())
               && keepsOrder(mc)
               && (!"java.util.Map".equals(mc.methodInfo().typeInfo().fullyQualifiedName())
                   || mc.parameterExpressions().size() % 2 == 0
                      && mc.parameterExpressions().stream().noneMatch(KotlinCollectionFactories::typedByTarget))
               && KotlinReadOnly.boundToReadOnly(mc);
    }

    /*
    Java's Set.of and Map.of iterate in an order of their own, salted per JVM; Kotlin's setOf and mapOf in insertion
    order. Code may depend on the first without knowing: langchain4j's ContentTest compares a Map.of with another Map.of
    entry by entry, which holds in Java (one JVM, one salt) and fails half the time with mapOf on one side. With two
    elements or more, Java's call stays; List.of keeps its order in both.
     */
    private static boolean keepsOrder(MethodCall mc) {
        String owner = mc.methodInfo().typeInfo().fullyQualifiedName();
        int n = mc.parameterExpressions().size();
        return "java.util.List".equals(owner) || "java.util.Set".equals(owner) && n <= 1 && !mc.methodInfo().isVarargs()
               || "java.util.Map".equals(owner) && n <= 2;
    }

    /*
    A value whose Kotlin type comes from what it initializes: an array or a generic construction, a lambda, a method
    reference. In Map.of(k, v) the expected type reaches v; in mapOf(k to v) the pair stands between, and fernflower's
    Map.of(EQ, new Integer[]{NE, null}) became an Array<Int> where the field holds Array<Int?>: kept as Java's call.
     */
    private static boolean typedByTarget(Expression e) {
        Expression u = KotlinExpressionPrinter.unwrap(e);
        return u instanceof io.codelaser.maddi.cst.api.expression.ConstructorCall
               || u instanceof io.codelaser.maddi.cst.api.expression.ArrayInitializer
               || u instanceof io.codelaser.maddi.cst.api.expression.Lambda
               || u instanceof io.codelaser.maddi.cst.api.expression.MethodReference
               || u instanceof io.codelaser.maddi.cst.api.expression.InlineConditional;
    }

    static OutputBuilder print(MethodCall mc, Qualification q) {
        String owner = mc.methodInfo().typeInfo().fullyQualifiedName();
        List<Expression> args = mc.parameterExpressions();
        String[] names = FACTORIES.get(owner);
        String typeArguments = mc.typeArguments().isEmpty() ? "" : mc.typeArguments().stream()
                .map(t -> KotlinTypeName.of(t, q)).collect(Collectors.joining(", ", "<", ">"));
        if (args.isEmpty()) {
            return new OutputBuilderImpl().add(new TextImpl(names[0] + typeArguments)).add(SymbolEnum.OPEN_CLOSE_PARENTHESIS);
        }
        OutputBuilder b = new OutputBuilderImpl().add(new TextImpl(names[1] + typeArguments));
        if (!"java.util.Map".equals(owner)) {
            return b.add(KotlinExpressionPrinter.arguments(args, mc.methodInfo(), mc, q));
        }
        // Map.of(k1, v1, k2, v2): mapOf(k1 to v1, k2 to v2); the keys and values are non-null, as Map.of demands
        b.add(SymbolEnum.LEFT_PARENTHESIS);
        for (int i = 0; i < args.size(); i += 2) {
            if (i > 0) b.add(SymbolEnum.COMMA);
            b.add(KotlinNullability.asserted(args.get(i), KotlinPrintCode.ASSERT_INTO_NON_NULL, q))
                    .add(KotlinSymbols.binary("to"))
                    .add(KotlinNullability.asserted(args.get(i + 1), KotlinPrintCode.ASSERT_INTO_NON_NULL, q));
        }
        return b.add(SymbolEnum.RIGHT_PARENTHESIS);
    }
}
