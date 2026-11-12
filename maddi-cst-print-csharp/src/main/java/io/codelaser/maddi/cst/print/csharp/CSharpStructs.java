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
package io.codelaser.maddi.cst.print.csharp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.analysis.ValueTypes;

/**
 * The program's classes and records that C# declares as a {@code readonly struct} or a {@code readonly record struct}:
 * small values, which C# copies instead of allocating them. The shared detection ({@link ValueTypes}) says which
 * classes have no identity, are not extended, and do not change after construction; here, what C# asks more:
 * <ul>
 *   <li>no obstacle at all, not even {@link ValueTypes.Obstacle#NULL_USED}: the struct is printed as itself, never
 *       {@code T?}, so a null that Java gives or compares it with would be C#'s {@code default}, or no C#;</li>
 *   <li>its own {@code equals}, or a record's: a struct's default {@code Equals} compares the fields, where Java's
 *       {@code Object.equals} compares the references;</li>
 *   <li>at most {@link #MAX_FIELDS} instance fields: a value is copied wherever it goes;</li>
 *   <li>a constructor with parameters, and none without: C#'s struct always has the default value, and its field
 *       initializers need a declared constructor (CS8983);</li>
 *   <li>no lambda, method reference, anonymous or local class in its instance members: they cannot use a struct's
 *       {@code this} (CS1673);</li>
 *   <li>not open to extension under the policy, no type parameter's bound, no delegate, no self-bounded type, no
 *       inner class that keeps its enclosing instance, no annotations (a library's binding expects a class);</li>
 *   <li>a member type, not a local or an anonymous one;</li>
 *   <li>never an {@code Optional}'s value: C# has it as {@code T?}, which for a struct is {@code Nullable<T>}, whose
 *       empty value the BCL's {@code FirstOrDefault} does not give;</li>
 *   <li>no cycle through instance fields of struct types: a struct cannot contain itself (CS0523).</li>
 * </ul>
 * C#'s {@code ==} on a struct is not Java's identity: the only {@code ==} the detection allows, {@code this == o} in
 * its {@code equals}, becomes {@code object.ReferenceEquals}, false for a struct, which skips the shortcut.
 */
final class CSharpStructs {

    static final int MAX_FIELDS = 4;

    private CSharpStructs() {
    }

    /** The structs among {@code all}, the program's types; {@code primaryTypes} and their member types may be one. */
    static Set<TypeInfo> analyze(Collection<TypeInfo> primaryTypes, List<TypeInfo> all) {
        ValueTypes valueTypes = ValueTypes.of(all);
        Set<TypeInfo> members = new HashSet<>();
        Deque<TypeInfo> todo = new ArrayDeque<>(primaryTypes);
        while (!todo.isEmpty()) {
            TypeInfo t = todo.pop();
            if (members.add(t)) todo.addAll(t.subTypes());
        }
        Set<TypeInfo> optional = optionalValues(all);
        Set<TypeInfo> structs = new HashSet<>();
        for (TypeInfo t : all) {
            if (members.contains(t) && !optional.contains(t) && struct(t, valueTypes.verdict(t))) structs.add(t);
        }
        removeCycles(structs);
        return Set.copyOf(structs);
    }

    /** The types that are an {@code Optional}'s value somewhere: a declaration's type or an expression's. */
    private static Set<TypeInfo> optionalValues(List<TypeInfo> all) {
        Set<TypeInfo> found = new HashSet<>();
        for (TypeInfo t : all) {
            for (FieldInfo f : t.fields()) {
                optionalValue(f.type(), found);
                if (f.initializer() != null && !f.initializer().isEmpty()) optionalValues(f.initializer(), found);
            }
            t.constructorAndMethodStream().forEach(m -> {
                optionalValue(m.returnType(), found);
                m.parameters().forEach(p -> optionalValue(p.parameterizedType(), found));
                if (m.methodBody() != null) optionalValues(m.methodBody(), found);
            });
        }
        return found;
    }

    private static void optionalValues(Element code, Set<TypeInfo> found) {
        code.visit((Element e) -> {
            if (e instanceof Expression x) optionalValue(x.parameterizedType(), found);
            if (e instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv -> optionalValue(lv.parameterizedType(), found));
            }
            return true;
        });
    }

    private static void optionalValue(ParameterizedType pt, Set<TypeInfo> found) {
        if (pt == null) return;
        if (CSharpTypeName.optional(pt) && !pt.parameters().isEmpty()) {
            ParameterizedType value = pt.parameters().getFirst();
            if (value.typeInfo() != null && !value.isTypeParameter()) found.add(value.typeInfo());
        }
        pt.parameters().forEach(a -> optionalValue(a, found));
    }

    private static boolean struct(TypeInfo t, ValueTypes.Verdict verdict) {
        if (verdict == null || !verdict.obstacles().isEmpty()) return false;
        boolean record = t.typeNature().isRecord();
        if (!record && !verdict.ownEquals()) return false;
        if (verdict.fields().size() > MAX_FIELDS) return false;
        if (!CSharpNames.translated(t) || !t.annotations().isEmpty()) return false;
        CSharpProgram program = CSharpContext.program();
        if (program.open(t) || program.boundsTypeParameter(t) || program.delegate(t)) return false;
        if (CSharpWildcardInterfaces.has(t) || CSharpInner.withOuter(t) || CSharpBcl.directSet(t)
            || CSharpNames.librarySingleAbstractMethod(t) != null) {
            return false;
        }
        List<MethodInfo> constructors = t.constructors().stream().filter(c -> !c.isSynthetic()
                && !CSharpTypePrinter.isImplicitDefaultConstructor(c)).toList();
        if (constructors.stream().anyMatch(c -> c.parameters().isEmpty())) return false;
        if (!record && constructors.isEmpty()) return false;
        return !capturesThis(t);
    }

    /** Code in an instance member that C# does not allow to use a struct's {@code this}, or a type of its own. */
    private static boolean capturesThis(TypeInfo t) {
        List<Element> code = new ArrayList<>();
        t.constructorAndMethodStream().filter(m -> !m.isStatic() && m.methodBody() != null)
                .forEach(m -> code.add(m.methodBody()));
        for (FieldInfo f : t.fields()) {
            if (!f.isStatic() && f.initializer() != null && !f.initializer().isEmpty()) code.add(f.initializer());
        }
        boolean[] found = {false};
        for (Element element : code) {
            element.visit((Element e) -> {
                if (e instanceof Lambda || e instanceof MethodReference || e instanceof LocalTypeDeclaration
                    || e instanceof ConstructorCall cc && cc.anonymousClass() != null) {
                    found[0] = true;
                }
                return !found[0];
            });
            if (found[0]) return true;
        }
        return false;
    }

    /** Drops each struct that contains itself through the instance fields of struct types, until none does. */
    private static void removeCycles(Set<TypeInfo> structs) {
        boolean changed = true;
        while (changed) {
            changed = false;
            Map<TypeInfo, Set<TypeInfo>> contained = new HashMap<>();
            for (TypeInfo t : structs) {
                Set<TypeInfo> direct = new HashSet<>();
                t.fields().stream().filter(f -> !f.isStatic()).map(FieldInfo::type)
                        .filter(pt -> pt.arrays() == 0 && !pt.isTypeParameter() && pt.typeInfo() != null)
                        .map(ParameterizedType::typeInfo).filter(structs::contains).forEach(direct::add);
                contained.put(t, direct);
            }
            for (TypeInfo t : List.copyOf(structs)) {
                if (reaches(t, t, contained, new HashSet<>())) {
                    structs.remove(t);
                    changed = true;
                    break;
                }
            }
        }
    }

    private static boolean reaches(TypeInfo from, TypeInfo target, Map<TypeInfo, Set<TypeInfo>> contained,
                                   Set<TypeInfo> seen) {
        for (TypeInfo next : contained.getOrDefault(from, Set.of())) {
            if (next == target) return true;
            if (seen.add(next) && reaches(next, target, contained, seen)) return true;
        }
        return false;
    }
}
