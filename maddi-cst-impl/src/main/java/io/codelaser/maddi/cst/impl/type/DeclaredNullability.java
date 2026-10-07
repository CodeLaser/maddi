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

package io.codelaser.maddi.cst.impl.type;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.analysis.NullAnnotations;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * What the SOURCE says about nullability: the null annotations written on a declaration and on the type arguments
 * of its type, read against the {@code @NullMarked} scope the declaration sits in. The answer is the declared type
 * with a {@link NullableState} on itself and on every type argument, recursively: the value shape of the
 * nullability design (maddi-mod {@code docs/design/nullability.md} §4.2, B2), so that what the source declares and
 * what the analysis infers can be compared position by position.
 * <p>
 * The annotations stay where the front end put them ({@link ParameterizedType#annotations()} for a type use,
 * {@link Element#annotations()} for a declaration): their identity is what a printer needs to write them back. This
 * class only INTERPRETS them; it does not write {@code nullable()} into the model.
 * <p>
 * <b>Recognition is by simple name</b> ({@link NullAnnotations}), e.g. elasticsearch's private
 * {@code org.elasticsearch.core.Nullable} too.
 * <p>
 * <b>Scope.</b> Inside a {@code @NullMarked} scope (the method, an enclosing type, or the package, the nearest
 * {@code @NullMarked}/{@code @NullUnmarked} winning) an unannotated type use is non-null, except a type-variable use
 * and an unbounded wildcard, whose nullability is parametric and stays {@link NullableState#UNSPECIFIED}. JSR-305's
 * {@code @ParametersAreNonnullByDefault} marks parameters only. Outside any marked scope an unannotated use is
 * {@link NullableState#UNSPECIFIED}. A primitive is always {@link NullableState#NONNULL}.
 * <p>
 * <b>maddi's own annotations</b> ({@code io.codelaser.maddi.annotation}) carry attributes that change their meaning:
 * {@code absent = true} denies the annotation, so it is read as not written; {@code @NotNull(content = true)} is
 * non-null AND non-null content, as maddi has always used it ({@code List.of}, {@code Map.of}, {@code stream()} in
 * the JDK hints): the type arguments become non-null too (unless a type-use annotation on an argument says
 * otherwise). Content of an array, and the return value of a functional interface (the annotation's other readings
 * of "content"), have no slot.
 * <p>
 * Known limitation: an array type is one {@link ParameterizedType}, so the array and its elements share one state;
 * the state is the ARRAY's, and an annotation on the elements ({@code @Nullable String[] a}) is not represented.
 * Another front-end fact: javac normalizes {@code ? extends Object} to {@code ?}, losing an annotation on that
 * bound; an unbounded wildcard is parametric anyway.
 */
public final class DeclaredNullability {

    private static final Set<String> NULLABLE = NullAnnotations.NULLABLE;
    private static final Set<String> NON_NULL = NullAnnotations.NON_NULL;
    private static final String NULL_MARKED = "NullMarked";
    private static final String NULL_UNMARKED = "NullUnmarked";
    private static final String PARAMETERS_NON_NULL_BY_DEFAULT = "ParametersAreNonnullByDefault";
    private static final String MADDI_ANNOTATIONS = "io.codelaser.maddi.annotation";

    private final Function<String, List<AnnotationExpression>> packageAnnotations;

    /**
     * @param packageAnnotations the annotations of a package's {@code package-info}, by package name; empty when
     *                           the package has none (the openjdk front end parses a package-info into a
     *                           {@link TypeInfo} whose type nature {@code isPackageInfo()})
     */
    public DeclaredNullability(Function<String, List<AnnotationExpression>> packageAnnotations) {
        this.packageAnnotations = packageAnnotations;
    }

    public ParameterizedType field(FieldInfo fieldInfo) {
        Scope scope = scopeOf(fieldInfo.owner(), null);
        return top(fieldInfo.type(), fieldInfo.annotations(), scope.marked);
    }

    public ParameterizedType parameter(ParameterInfo parameterInfo) {
        MethodInfo methodInfo = parameterInfo.methodInfo();
        Scope scope = scopeOf(methodInfo.typeInfo(), methodInfo);
        return top(parameterInfo.parameterizedType(), parameterInfo.annotations(),
                scope.marked || scope.parametersNonNull);
    }

    /** The declared return type; {@code void} and constructors give {@code null}. */
    public ParameterizedType returnType(MethodInfo methodInfo) {
        if (methodInfo.isConstructor() || methodInfo.returnType().isVoid()) return null;
        Scope scope = scopeOf(methodInfo.typeInfo(), methodInfo);
        return top(methodInfo.returnType(), methodInfo.annotations(), scope.marked);
    }

    private ParameterizedType top(ParameterizedType declared, List<AnnotationExpression> declarationAnnotations,
                                  boolean marked) {
        ParameterizedType withArguments = arguments(declared, marked);
        NullableState fromTypeUse = explicit(declared.annotations());
        // On an array, a TYPE_USE annotation written in declaration position ('@Nullable String[] a') qualifies the
        // ELEMENTS (JLS 9.7.4), but javac stores it with the declaration's annotations: only 'String @Nullable [] a'
        // reaches the type. The element state has no slot (class comment), so such annotations are skipped here;
        // a pure declaration annotation (JSR-305's @Nullable) still speaks about the array itself.
        List<AnnotationExpression> aboutTheDeclaration = declared.arrays() == 0 ? declarationAnnotations
                : declarationAnnotations.stream().filter(ae -> !isTypeUse(ae)).toList();
        NullableState state = fromTypeUse != null ? fromTypeUse : explicit(aboutTheDeclaration);
        ParameterizedType withContent = contentNonNull(declarationAnnotations) ? nonNullContent(withArguments)
                : withArguments;
        return withContent.withNullable(state != null ? state : implicit(declared, marked));
    }

    // maddi's @NotNull(content = true): the type arguments are non-null too, except where one is annotated itself
    private static ParameterizedType nonNullContent(ParameterizedType pt) {
        if (pt.parameters().isEmpty() || pt.arrays() > 0) return pt;
        return pt.withParameters(pt.parameters().stream()
                .map(p -> explicit(p.annotations()) != null ? p : p.withNullable(NullableState.NONNULL))
                .toList());
    }

    private static boolean contentNonNull(List<AnnotationExpression> annotations) {
        return annotations.stream().anyMatch(ae -> isMaddi(ae) && NON_NULL.contains(ae.typeInfo().simpleName())
                                                   && ae.extractBoolean("content") && !ae.extractBoolean("absent"));
    }

    private static boolean isMaddi(AnnotationExpression ae) {
        return MADDI_ANNOTATIONS.equals(ae.typeInfo().packageName());
    }

    private ParameterizedType use(ParameterizedType pt, boolean marked) {
        ParameterizedType withArguments = arguments(pt, marked);
        NullableState state = explicit(pt.annotations());
        return withArguments.withNullable(state != null ? state : implicit(pt, marked));
    }

    private ParameterizedType arguments(ParameterizedType pt, boolean marked) {
        if (pt.parameters().isEmpty()) return pt;
        return pt.withParameters(pt.parameters().stream().map(p -> use(p, marked)).toList());
    }

    private static NullableState implicit(ParameterizedType pt, boolean marked) {
        if (pt.isPrimitiveExcludingVoid() && pt.arrays() == 0) return NullableState.NONNULL;
        if (!marked) return NullableState.UNSPECIFIED;
        // parametric: a type variable or a wildcard takes its nullability from the instantiation
        if (pt.typeParameter() != null && pt.arrays() == 0) return NullableState.UNSPECIFIED;
        if (pt.wildcard() != null && pt.wildcard().isUnbound()) return NullableState.UNSPECIFIED;
        return NullableState.NONNULL;
    }

    // null when the annotations say nothing; NULLABLE wins when both kinds are present (a conflict is the user's,
    // and the safe reading is the nullable one)
    static NullableState explicit(List<AnnotationExpression> annotations) {
        boolean nonNull = false;
        for (AnnotationExpression ae : annotations) {
            // maddi's: 'absent = true' denies the annotation ('content = true' adds the content: top)
            if (isMaddi(ae) && ae.extractBoolean("absent")) continue;
            String name = ae.typeInfo().simpleName();
            if (NULLABLE.contains(name)) return NullableState.NULLABLE;
            if (NON_NULL.contains(name)) nonNull = true;
        }
        return nonNull ? NullableState.NONNULL : null;
    }

    // the annotation type's @Target includes TYPE_USE
    private static boolean isTypeUse(AnnotationExpression ae) {
        return ae.typeInfo().annotations().stream()
                .filter(target -> "Target".equals(target.typeInfo().simpleName()))
                .anyMatch(target -> target.keyValuePairs().stream()
                        .anyMatch(kv -> String.valueOf(kv.value()).contains("TYPE_USE")));
    }

    private record Scope(boolean marked, boolean parametersNonNull) {
    }

    // innermost first: the method, the type and its enclosing types, the package
    private Scope scopeOf(TypeInfo typeInfo, MethodInfo methodInfo) {
        Boolean marked = methodInfo == null ? null : markedBy(methodInfo.annotations());
        boolean parametersNonNull = methodInfo != null && has(methodInfo.annotations(), PARAMETERS_NON_NULL_BY_DEFAULT);
        TypeInfo t = typeInfo;
        while (t != null) {
            if (marked == null) marked = markedBy(t.annotations());
            parametersNonNull |= has(t.annotations(), PARAMETERS_NON_NULL_BY_DEFAULT);
            t = t.compilationUnitOrEnclosingType().isRight() ? t.compilationUnitOrEnclosingType().getRight() : null;
        }
        List<AnnotationExpression> pkg = packageAnnotations.apply(typeInfo.packageName());
        if (marked == null) marked = markedBy(pkg);
        parametersNonNull |= has(pkg, PARAMETERS_NON_NULL_BY_DEFAULT);
        return new Scope(Boolean.TRUE.equals(marked), parametersNonNull);
    }

    private static Boolean markedBy(List<AnnotationExpression> annotations) {
        if (has(annotations, NULL_UNMARKED)) return false;
        if (has(annotations, NULL_MARKED)) return true;
        return null;
    }

    private static boolean has(List<AnnotationExpression> annotations, String simpleName) {
        return annotations.stream().anyMatch(ae -> simpleName.equals(ae.typeInfo().simpleName()));
    }
}
