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

import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.ClassExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.List;
import java.util.Set;

/**
 * A declaration's Java annotations, as Kotlin writes them: {@code @Timeout(value = 60, unit = TimeUnit.SECONDS)},
 * a class literal {@code X::class}, an array {@code [a, b]}. JUnit finds a translated test by its {@code @Test}.
 * <p>
 * Left out: what Kotlin says otherwise ({@code @Override} is the {@code override} modifier, a nullability annotation
 * the type's {@code ?}) or does not need ({@code @SuppressWarnings}, {@code @FunctionalInterface},
 * {@code @SafeVarargs}). {@code @Deprecated} is Kotlin's own, which wants a message.
 */
final class KotlinAnnotations {

    private static final Set<String> LEFT_OUT = Set.of("java.lang.Override", "java.lang.SuppressWarnings",
            "java.lang.FunctionalInterface", "java.lang.SafeVarargs");
    private static final Set<String> NULLABILITY = Set.of("NotNull", "Nullable", "NonNull", "NullMarked",
            "NullUnmarked", "CheckForNull", "Nonnull", "ParametersAreNonnullByDefault");

    private KotlinAnnotations() {
    }

    /** The annotations, each followed by a space; empty when there are none to print, or not translating Java. */
    static OutputBuilder print(List<AnnotationExpression> annotations, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl();
        if (!KotlinContext.translatingJava()) return b;
        for (AnnotationExpression ae : annotations) {
            if (ae.typeInfo() == null) continue;
            String fqn = ae.typeInfo().fullyQualifiedName();
            if (LEFT_OUT.contains(fqn) || NULLABILITY.contains(ae.typeInfo().simpleName())) continue;
            if ("java.lang.Deprecated".equals(fqn)) {
                b.add(new TextImpl("@Deprecated(\"\")")).add(SpaceEnum.ONE);
                continue;
            }
            b.add(annotation(ae, "@", q)).add(SpaceEnum.ONE);
        }
        return b;
    }

    /** The name and the arguments; as an annotation's argument, without the {@code @}. */
    private static OutputBuilder annotation(AnnotationExpression ae, String at, Qualification q) {
        OutputBuilder b = new OutputBuilderImpl().add(new TextImpl(at + KotlinTypeName.name(ae.typeInfo(), q)));
        List<AnnotationExpression.KV> kvs = ae.keyValuePairs();
        if (kvs.isEmpty()) return b;
        b.add(SymbolEnum.LEFT_PARENTHESIS);
        boolean positional = kvs.size() == 1 && (kvs.getFirst().keyIsDefault() || "value".equals(kvs.getFirst().key()));
        for (int i = 0; i < kvs.size(); i++) {
            if (i > 0) b.add(SymbolEnum.COMMA);
            AnnotationExpression.KV kv = kvs.get(i);
            if (!positional) b.add(new TextImpl(kv.key())).add(SpaceEnum.ONE).add(new TextImpl("=")).add(SpaceEnum.ONE);
            b.add(value(kv.value(), q));
        }
        return b.add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    private static OutputBuilder value(Expression e, Qualification q) {
        return switch (e) {
            case ClassExpression ce -> new OutputBuilderImpl().add(new TextImpl(classLiteral(ce.type(), q)));
            case ArrayInitializer ai -> {
                OutputBuilder b = new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACKET);
                for (int i = 0; i < ai.expressions().size(); i++) {
                    if (i > 0) b.add(SymbolEnum.COMMA);
                    b.add(value(ai.expressions().get(i), q));
                }
                yield b.add(SymbolEnum.RIGHT_BRACKET);
            }
            case AnnotationExpression nested -> annotation(nested, "", q);
            default -> KotlinExpressionPrinter.print(e, q);
        };
    }

    /** An annotation element's type: a class is a {@code KClass}, {@code Class<?>[]} an {@code Array<KClass<*>>}. */
    static String elementType(ParameterizedType type, Qualification q) {
        if (type.typeInfo() != null && "java.lang.Class".equals(type.typeInfo().fullyQualifiedName())) {
            String argument = type.parameters().isEmpty() || type.parameters().getFirst().isUnboundWildcard() ? "*"
                    : "out " + KotlinTypeName.of(type.parameters().getFirst().withWildcard(null), q);
            String kclass = "kotlin.reflect.KClass<" + argument + ">";
            return type.arrays() > 0 ? "Array<" + kclass + ">" : kclass;
        }
        return KotlinTypeName.of(type, q);
    }

    /** An element's default: {@code String[] value() default ""} is {@code [""]}. */
    static OutputBuilder elementValue(Expression e, boolean array, Qualification q) {
        if (array && !(e instanceof ArrayInitializer)) {
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_BRACKET).add(value(e, q)).add(SymbolEnum.RIGHT_BRACKET);
        }
        return value(e, q);
    }

    /** In an annotation, a class is a KClass: {@code X::class}, not {@code X::class.java}. */
    private static String classLiteral(ParameterizedType type, Qualification q) {
        if (type.arrays() > 0 || type.typeInfo() == null) return KotlinTypeName.of(type, q) + "::class";
        return KotlinTypeName.name(type.typeInfo(), q) + "::class";
    }
}
