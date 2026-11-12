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
package io.codelaser.maddi.cst.print.kotlin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.analysis.ValueTypes;
import io.codelaser.maddi.cst.print.translate.CstQueries;

/**
 * The Java records that Kotlin declares as a {@code @JvmInline value class} instead of a {@code data class}: a wrapper
 * of one value, which the JVM passes unboxed. The shared detection ({@link ValueTypes}) says which records need no
 * identity; Kotlin asks more:
 * <ul>
 *   <li>none of the identity obstacles ({@link ValueTypes.Verdict#identityFree()}): Kotlin forbids {@code ===} on a
 *       value class, and a lock or an identity hash would see a box. A null is fine: {@code T?} boxes;</li>
 *   <li>exactly one component, of another type, and no type parameters;</li>
 *   <li>no {@code equals} or {@code hashCode} of its own: a value class compares its value, as the record does, and
 *       may not redeclare them;</li>
 *   <li>no annotations, on the record or its component, and no inner class;</li>
 *   <li>seen only in its own file: private, or nested in a private type, or local. The translation is a drop-in
 *       replacement for Java's class files, and Java code cannot call a value class's constructor, nor a function
 *       that takes one, whose JVM name Kotlin mangles. All uses are then in the file, which is also what the
 *       detection is given;</li>
 *   <li>never a vararg's type (Kotlin forbids it), nor that of a field without a value that is not final, nor of a
 *       local declared without one: those can be {@code lateinit}, which a value class cannot be.</li>
 * </ul>
 * The decision is per primary type, kept for the file being printed.
 */
final class KotlinValueClasses {

    private static final ThreadLocal<Map.Entry<TypeInfo, Set<TypeInfo>>> FILE = new ThreadLocal<>();

    private KotlinValueClasses() {
    }

    /** The record is printed as a {@code @JvmInline value class}. */
    static boolean valueClass(TypeInfo typeInfo) {
        if (!typeInfo.typeNature().isRecord() || typeInfo.isAnonymous() || !KotlinContext.translatingJava()) {
            return false;
        }
        TypeInfo primary = typeInfo.primaryType();
        Map.Entry<TypeInfo, Set<TypeInfo>> file = FILE.get();
        if (file == null || file.getKey() != primary) {
            file = Map.entry(primary, analyze(primary));
            FILE.set(file);
        }
        return file.getValue().contains(typeInfo);
    }

    private static Set<TypeInfo> analyze(TypeInfo primary) {
        List<TypeInfo> all = CstQueries.allTypes(primary);
        Set<TypeInfo> candidates = new HashSet<>();
        for (TypeInfo t : all) {
            if (t.typeNature().isRecord() && !t.isAnonymous() && seenInFileOnly(t)) candidates.add(t);
        }
        if (candidates.isEmpty()) return Set.of();
        ValueTypes valueTypes = ValueTypes.of(all);
        Set<TypeInfo> excluded = excludedByUses(all, candidates);
        Set<TypeInfo> valueClasses = new HashSet<>();
        for (TypeInfo t : candidates) {
            if (!excluded.contains(t) && shape(t, valueTypes.verdict(t))) valueClasses.add(t);
        }
        return Set.copyOf(valueClasses);
    }

    private static boolean seenInFileOnly(TypeInfo t) {
        return KotlinTypePrinter.isLocal(t) || !t.isPrimaryType() && t.effectiveAccess().isPrivate();
    }

    private static boolean shape(TypeInfo t, ValueTypes.Verdict verdict) {
        if (verdict == null || !verdict.identityFree() || verdict.ownEquals()) return false;
        if (!t.typeParameters().isEmpty() || !t.annotations().isEmpty()) return false;
        if (verdict.fields().size() != 1) return false;
        FieldInfo component = verdict.fields().getFirst();
        if (!component.annotations().isEmpty() || component.type().typeInfo() == t && component.type().arrays() == 0) {
            return false;
        }
        if (t.methods().stream().anyMatch(m -> "hashCode".equals(m.name()) && m.parameters().isEmpty() && !m.isStatic()
                                            && !m.isSynthetic())) {
            return false;
        }
        return t.subTypes().stream().noneMatch(TypeInfo::isInnerClass);
    }

    /** The candidates a vararg, a field that may be lateinit, or a local declared without a value has as its type. */
    private static Set<TypeInfo> excludedByUses(List<TypeInfo> all, Set<TypeInfo> candidates) {
        Set<TypeInfo> excluded = new HashSet<>();
        for (TypeInfo t : all) {
            t.constructorAndMethodStream().forEach(m -> {
                m.parameters().stream().filter(p -> p.isVarArgs())
                        .forEach(p -> exclude(p.parameterizedType(), candidates, excluded));
                if (m.methodBody() != null) localsWithoutValue(m.methodBody(), candidates, excluded);
            });
            for (FieldInfo f : t.fields()) {
                boolean hasInitializer = f.initializer() != null && !f.initializer().isEmpty();
                if (!hasInitializer && !KotlinFieldPrinter.isFinal(f)) exclude(f.type(), candidates, excluded);
                if (hasInitializer) localsWithoutValue(f.initializer(), candidates, excluded);
            }
        }
        return excluded;
    }

    private static void localsWithoutValue(Element code, Set<TypeInfo> candidates, Set<TypeInfo> excluded) {
        code.visit((Element e) -> {
            if (e instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().filter(lv -> lv.assignmentExpression() == null
                                                       || lv.assignmentExpression().isEmpty())
                        .forEach(lv -> exclude(lv.parameterizedType(), candidates, excluded));
            }
            return true;
        });
    }

    private static void exclude(ParameterizedType pt, Set<TypeInfo> candidates, Set<TypeInfo> excluded) {
        if (pt != null && pt.typeInfo() != null && candidates.contains(pt.typeInfo())) excluded.add(pt.typeInfo());
    }
}
