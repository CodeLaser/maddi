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


package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The null annotations, recognised by SIMPLE NAME, the way the families are actually used: JSpecify, JetBrains,
 * JSR-305, Jakarta, Checker Framework, FindBugs/SpotBugs, Android, Lombok, Eclipse JDT, maddi's own, and a project's
 * private copy all spell nullable as {@code Nullable} or {@code CheckForNull}, non-null as {@code NonNull},
 * {@code NotNull} or {@code Nonnull}. Shared by the reading of what the source declares
 * ({@code DeclaredNullability}), the nullability pass (a declaration so annotated is a contract) and the annotation
 * decorator (it does not add a second one).
 */
public final class NullAnnotations {
    public static final Set<String> NULLABLE = Set.of("Nullable", "CheckForNull", "NullableDecl", "NullableType");
    public static final Set<String> NON_NULL = Set.of("NonNull", "NotNull", "Nonnull", "NonNullDecl", "NonNullType");

    private NullAnnotations() {
    }

    public static boolean isNullnessAnnotation(AnnotationExpression ae) {
        String name = ae.typeInfo().simpleName();
        return NULLABLE.contains(name) || NON_NULL.contains(name);
    }

    /** A field, parameter or method (its return) whose declaration or declared type carries a null annotation. */
    public static boolean hasNullnessAnnotation(Info info) {
        return annotations(info).anyMatch(NullAnnotations::isNullnessAnnotation);
    }

    /**
     * What the null annotations on a field, parameter or method (its return) state explicitly, ignoring any
     * {@code @NullMarked} scope: NULLABLE (which wins over a conflicting non-null), NONNULL, or null for nothing.
     * maddi's {@code absent = true} denies the annotation. (An element annotation on an array written in
     * declaration position is read as being about the array.)
     */
    public static NullableState explicitState(Info info) {
        boolean nonNull = false;
        for (AnnotationExpression ae : annotations(info).toList()) {
            if (MADDI.equals(ae.typeInfo().packageName()) && ae.extractBoolean("absent")) continue;
            String name = ae.typeInfo().simpleName();
            if (NULLABLE.contains(name)) return NullableState.NULLABLE;
            if (NON_NULL.contains(name)) nonNull = true;
        }
        return nonNull ? NullableState.NONNULL : null;
    }

    private static final String MADDI = "io.codelaser.maddi.annotation";

    /**
     * Whether {@code methodInfo} sits in a JSpecify {@code @NullMarked} scope as a class file declares it: the
     * method, its type and the enclosing types, then the package and the module of the type's compilation unit
     * ({@link CompilationUnit#packageAnnotations()}); the nearest
     * {@code @NullMarked}/{@code @NullUnmarked} wins. For a library method: a source package's package-info is a
     * type of its own, which {@code DeclaredNullability} reads.
     */
    public static boolean inNullMarkedScope(MethodInfo methodInfo) {
        Boolean marked = markedBy(methodInfo.annotations());
        TypeInfo t = methodInfo.typeInfo();
        while (marked == null && t != null) {
            marked = markedBy(t.annotations());
            t = t.compilationUnitOrEnclosingType().isRight() ? t.compilationUnitOrEnclosingType().getRight() : null;
        }
        CompilationUnit cu = methodInfo.typeInfo().primaryType().compilationUnit();
        if (marked == null && cu != null) marked = markedBy(cu.packageAnnotations());
        if (marked == null && cu != null) marked = markedBy(cu.moduleAnnotations());
        return Boolean.TRUE.equals(marked);
    }

    private static Boolean markedBy(List<AnnotationExpression> annotations) {
        if (annotations.stream().anyMatch(ae -> "NullUnmarked".equals(ae.typeInfo().simpleName()))) return false;
        if (annotations.stream().anyMatch(ae -> "NullMarked".equals(ae.typeInfo().simpleName()))) return true;
        return null;
    }

    private static Stream<AnnotationExpression> annotations(Info info) {
        ParameterizedType type = switch (info) {
            case FieldInfo fi -> fi.type();
            case ParameterInfo pi -> pi.parameterizedType();
            case MethodInfo mi -> mi.returnType();
            default -> null;
        };
        return Stream.concat(info.annotations().stream(), type == null ? Stream.empty() : type.annotations().stream());
    }
}
