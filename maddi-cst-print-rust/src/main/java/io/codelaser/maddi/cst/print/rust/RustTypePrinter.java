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

package io.codelaser.maddi.cst.print.rust;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.EmptyExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.print.rust.RustFacts.Passing;
import io.codelaser.maddi.cst.print.rust.RustFacts.Receiver;
import io.codelaser.maddi.cst.print.rust.RustTypes.Form;
import io.codelaser.maddi.cst.print.translate.CstQueries;

/**
 * A Java type as Rust items: a class is a {@code struct} with an inherent {@code impl}, plus an {@code impl Trait for}
 * per source interface it implements; an interface is a {@code trait}; a record a {@code struct} with public fields and
 * accessors; an enum without state a plain {@code enum}. Nested types become items of their own.
 * <p>
 * A constructor is an associated function returning {@code Self}. When its body only assigns fields (and declares
 * locals), the fields become the initializers of one {@code Self { … }}; otherwise it builds {@code this}, a local
 * with every field at its empty value, runs the body on it, and returns it.
 */
final class RustTypePrinter {
    private static final String INDENT = "    ";

    private final RustContext context;

    /** The parent's type when the struct being printed has a source superclass: its {@code base} field. */
    private String base;

    /** The name {@link #signature} gives the method, where it is not {@link RustNames#method}'s: an implementation. */
    private String methodName;

    /** The root of a closed hierarchy's {@code kind} in its constructor's literal ({@link RustHierarchies}). */
    private String kindInit;

    RustTypePrinter(RustContext context) {
        this.context = context;
    }

    /** The items of {@code typeInfo} and of its nested types. */
    String print(TypeInfo typeInfo) {
        StringBuilder sb = new StringBuilder();
        TypeInfo outer = context.type;
        context.type = typeInfo;
        try {
            if (typeInfo.typeNature().isAnnotation()) return "";
            RustHierarchies hierarchies = RustHierarchies.current();
            if (hierarchies.isSubMember(typeInfo)) {
                // printed with its root, as a variant of the root's kind
            } else if (hierarchies.isMember(typeInfo)) sb.append(hierarchyType(typeInfo));
            else if (typeInfo.isInterface()) sb.append(trait(typeInfo));
            else if (typeInfo.typeNature().isEnum()) sb.append(enumType(typeInfo));
            else sb.append(structType(typeInfo));
        } finally {
            context.type = outer;
        }
        sb.append(statics);
        statics.setLength(0);
        for (TypeInfo sub : typeInfo.subTypes()) {
            if (sub.isSynthetic()) continue;
            sb.append("\n").append(print(sub));
        }
        return sb.toString();
    }

    /** No instance fields and no constructor with parameters: Rust's {@code enum} of unit variants, {@code Copy}. */
    /**
     * An enum's field that its one constructor sets from a parameter, and every constant from a literal, a primitive
     * or a string: the literals in the constants' order. {@code null} for any other field.
     */
    static List<Expression> constantField(FieldInfo f) {
        TypeInfo e = f.owner();
        if (f.isStatic() || !e.typeNature().isEnum() || !isFinal(f) || e.constructors().size() != 1) return null;
        ParameterizedType type = f.type();
        if (type.arrays() > 0 || !type.isPrimitiveExcludingVoid() && !RustTypes.isString(type)) return null;
        MethodInfo constructor = e.constructors().getFirst();
        int index = -1;
        for (Statement s : constructor.methodBody().statements()) {
            if (s instanceof ExpressionAsStatement eas && eas.expression() instanceof Assignment a
                && a.variableTarget() instanceof FieldReference fr && fr.fieldInfo() == f) {
                if (index >= 0 || !(CstQueries.unwrap(a.value()) instanceof VariableExpression ve
                                    && ve.variable() instanceof ParameterInfo pi)) {
                    return null;
                }
                index = pi.index();
            }
        }
        if (index < 0) return null;
        List<Expression> literals = new ArrayList<>();
        for (FieldInfo constant : e.fields()) {
            if (!constant.isStatic() || constant.type().typeInfo() != e) continue;
            if (!(constant.initializer() instanceof ConstructorCall cc) || cc.parameterExpressions().size() <= index
                || !(cc.parameterExpressions().get(index) instanceof ConstantExpression<?> literal)) {
                return null;
            }
            literals.add(literal);
        }
        return literals;
    }

    /** The method that reads a {@link #constantField}: {@code gravity_value()}. */
    static String constantFieldMethod(FieldInfo f) {
        return RustNames.snake(f.name()) + "_value";
    }

    static boolean plainEnum(TypeInfo typeInfo) {
        return typeInfo.fields().stream().noneMatch(f -> !f.isStatic() && !f.isSynthetic())
               && typeInfo.constructors().stream().allMatch(c -> c.parameters().isEmpty());
    }

    // ---- items

    private String trait(TypeInfo typeInfo) {
        StringBuilder sb = new StringBuilder(doc(typeInfo));
        sb.append(visibility(typeInfo.isPrivate())).append("trait ").append(RustNames.type(typeInfo))
                .append(generics(typeInfo.typeParameters())).append(supertraits(typeInfo)).append(" {\n");
        // the interface's constants and static state: module-level items beside the trait (statics)
        for (FieldInfo f : typeInfo.fields()) {
            if (!f.isSynthetic()) staticField(f);
        }
        for (MethodInfo m : typeInfo.methods()) {
            if (m.isSynthetic()) continue;
            if (m.isStatic()) {
                context.message(RustPrintCode.UNSUPPORTED, m, "static method of an interface");
                continue;
            }
            Block body = m.methodBody();
            boolean bodyless = m.isAbstract() || body == null || body.isEmpty();
            // a declaration without a body has no bindings: no 'mut' on its parameters
            String signature = signature(m, false);
            // a generic method has no place in a vtable: kept out of it, the trait stays dyn compatible (E0038), and
            // only a call of the method on a Box<dyn ..> is rustc's error
            if (!m.typeParameters().isEmpty()) signature += " where Self: Sized";
            sb.append(INDENT).append(bodyless ? signature.replaceAll("\\bmut ((r#)?\\w+): ", "$1: ") : signature);
            if (bodyless) sb.append(";\n");
            else sb.append(" {\n").append(body(m, 2)).append(INDENT).append("}\n");
            if (bodyless && context.mutableGetters.contains(m)) sb.append(INDENT).append(mutableGetterSignature(m)).append(";\n");
        }
        for (TypeInfo struct : RustHierarchies.current().downcasts(typeInfo)) sb.append(conversions(struct, false));
        sb.append("}\n");
        MethodInfo sam = functionalMethod(typeInfo);
        if (sam != null && context.facts.receiver(sam) != Receiver.OWNED && !context.facts.resultBorrowsReceiver(sam)) {
            // a lambda implements the interface: every closure of the method's shape implements the trait
            boolean mutable = context.facts.receiver(sam) == Receiver.EXCLUSIVE;
            String signature = signature(sam, false).replaceAll("\\bmut ((r#)?\\w+): ", "$1: ");
            String names = sam.parameters().stream().map(p -> RustNames.snake(p.name())).collect(Collectors.joining(", "));
            sb.append("\nimpl<F: ").append(mutable ? "FnMut(" : "Fn(")
                    .append(String.join(", ", closureParameters(typeInfo, context))).append(")")
                    .append(resultType(sam)).append("> ").append(RustNames.type(typeInfo)).append(" for F {\n")
                    .append(INDENT).append(signature).append(" {\n")
                    .append(INDENT).append(INDENT).append("(*self)(").append(names).append(")\n")
                    .append(INDENT).append("}\n}\n");
        }
        return sb.toString();
    }

    /**
     * The method of a source interface a lambda may implement: its only method, abstract, without type parameters, in
     * an interface without type parameters or source super-interfaces. {@code null} for another type.
     */
    static MethodInfo functionalMethod(TypeInfo ti) {
        if (ti == null || !ti.isInterface() || ti.compilationUnit().externalLibrary() || !ti.typeParameters().isEmpty()
            || ti.interfacesImplemented().stream().anyMatch(i -> !i.typeInfo().compilationUnit().externalLibrary())) {
            return null;
        }
        List<MethodInfo> methods = ti.methods().stream().filter(m -> !m.isSynthetic() && !m.isStatic()).toList();
        if (methods.size() != 1) return null;
        MethodInfo m = methods.getFirst();
        return m.isAbstract() && m.typeParameters().isEmpty() ? m : null;
    }

    /** The parameter types of {@link #functionalMethod}, as its trait declares them. */
    static List<String> closureParameters(TypeInfo ti, RustContext context) {
        MethodInfo m = functionalMethod(ti);
        if (m == null) return null;
        RustTypePrinter printer = new RustTypePrinter(context);
        return m.parameters().stream().map(p -> printer.parameter(m, p).replaceFirst("^(mut )?(r#)?\\w+: ", "")).toList();
    }

    private String supertraits(TypeInfo typeInfo) {
        List<String> supers = typeInfo.interfacesImplemented().stream()
                .filter(i -> !i.typeInfo().compilationUnit().externalLibrary())
                .map(i -> RustTypes.owned(i, context, typeInfo).replaceFirst("^Box<dyn (.*)>$", "$1"))
                .toList();
        return supers.isEmpty() ? "" : ": " + String.join(" + ", supers);
    }

    private String enumType(TypeInfo typeInfo) {
        List<FieldInfo> state = typeInfo.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).toList();
        if (!plainEnum(typeInfo) && !state.stream().allMatch(f -> constantField(f) != null)) {
            context.message(RustPrintCode.ENUM_WITH_STATE, typeInfo, typeInfo.simpleName());
        }
        List<String> variants = typeInfo.fields().stream()
                .filter(f -> f.isStatic() && f.type().typeInfo() == typeInfo)
                .map(f -> RustNames.variant(f.name())).toList();
        StringBuilder sb = new StringBuilder(doc(typeInfo));
        sb.append("#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]\n")
                .append(visibility(typeInfo.isPrivate())).append("enum ").append(RustNames.type(typeInfo)).append(" {\n");
        variants.forEach(v -> sb.append(INDENT).append(v).append(",\n"));
        sb.append("}\n");
        // what java.lang.Enum gives every enum: values(), ordinal(), name()
        List<String> constants = typeInfo.fields().stream()
                .filter(f -> f.isStatic() && f.type().typeInfo() == typeInfo).map(FieldInfo::name).toList();
        int n = variants.size();
        sb.append("\nimpl ").append(RustNames.type(typeInfo)).append(" {\n");
        sb.append(INDENT).append("pub const VALUES: [Self; ").append(n).append("] = [")
                .append(variants.stream().map(v -> "Self::" + v).collect(Collectors.joining(", "))).append("];\n");
        sb.append(INDENT).append("pub fn values() -> [Self; ").append(n).append("] {\n").append(INDENT).append(INDENT)
                .append("Self::VALUES\n").append(INDENT).append("}\n");
        sb.append(INDENT).append("pub fn ordinal(self) -> i32 {\n").append(INDENT).append(INDENT)
                .append("self as i32\n").append(INDENT).append("}\n");
        sb.append(INDENT).append("pub fn name(self) -> &'static str {\n").append(INDENT).append(INDENT)
                .append("match self {\n");
        for (int i = 0; i < n; i++) {
            sb.append(INDENT.repeat(3)).append("Self::").append(variants.get(i)).append(" => \"")
                    .append(constants.get(i)).append("\",\n");
        }
        sb.append(INDENT).append(INDENT).append("}\n").append(INDENT).append("}\n");
        // a field each constant sets to a literal: a match over the variants
        for (FieldInfo f : state) {
            List<Expression> literals = constantField(f);
            if (literals == null) continue;
            String type = RustTypes.isString(f.type()) ? "&'static str" : RustTypes.primitive(f.type().typeInfo());
            sb.append(INDENT).append("fn ").append(constantFieldMethod(f)).append("(self) -> ").append(type)
                    .append(" {\n").append(INDENT).append(INDENT).append("match self {\n");
            for (int i = 0; i < n; i++) {
                sb.append(INDENT.repeat(3)).append("Self::").append(variants.get(i)).append(" => ")
                        .append(new RustBodyPrinter(context).text(literals.get(i))).append(",\n");
            }
            sb.append(INDENT).append(INDENT).append("}\n").append(INDENT).append("}\n");
        }
        for (MethodInfo m : typeInfo.methods()) {
            if (!m.isSynthetic() && !m.isStaticInitializer() && !m.isInstanceInitializer()) sb.append(method(m));
        }
        return sb.append("}\n").toString();
    }

    private String structType(TypeInfo typeInfo) {
        boolean record = typeInfo.typeNature().isRecord();
        ParameterizedType parent = typeInfo.parentClass();
        TypeInfo base = sourceParent(typeInfo);
        if (base != null) {
            context.message(RustPrintCode.INHERITANCE_AS_COMPOSITION, typeInfo, base.fullyQualifiedName());
        } else if (parent != null && !parent.typeInfo().isJavaLangObject() && !parent.typeInfo().fullyQualifiedName()
                .equals("java.lang.Record")) {
            context.message(RustPrintCode.INHERITANCE, typeInfo, parent.typeInfo().fullyQualifiedName());
        }
        String baseType = base == null ? collectionBase(typeInfo, context) : RustTypes.owned(parent, context, typeInfo);
        // the parent's part of the object, owned by it: not the Rc an object of the parent class is shared in
        if (baseType != null && baseType.startsWith("Rc<") && baseType.endsWith(">")) {
            baseType = baseType.substring("Rc<".length(), baseType.length() - 1);
        }
        this.base = baseType;
        List<FieldInfo> fields = typeInfo.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).toList();
        String name = RustNames.type(typeInfo);
        boolean typestate = RustTypestate.typestate(typeInfo, context);
        String generics = typestate ? "<" + RustTypestate.STATE + ">"
                : generics(typeInfo.typeParameters(), context.borrows.hasLifetime(typeInfo));
        // the trait bounds its members need (RustBounds); @Immutable(hc=true) shares its hidden content by cloning
        String implGenerics = typestate ? generics
                : bounded(typeInfo.typeParameters(), context.borrows.hasLifetime(typeInfo));
        List<String> phantom = new ArrayList<>(phantomTypeParameters(typeInfo, fields));
        // a JDK collection's state holds its type parameters (Vec<E>)
        String collection = base == null ? baseType : null;
        if (collection != null) phantom.removeIf(tp -> collection.matches(".*\\b" + tp + "\\b.*"));
        if (typestate) {
            // the state of the transition, a type that no field holds
            phantom.add(RustTypestate.STATE);
            context.use("crate::jrt::" + RustTypestate.BEFORE);
            context.use("crate::jrt::" + RustTypestate.AFTER);
            context.message(RustPrintCode.TYPESTATE, typeInfo, typeInfo.simpleName());
        }

        StringBuilder sb = new StringBuilder(doc(typeInfo));
        // derive(PartialEq) bounds T by PartialEq, where a HashSet<T> compares only with T: Eq + Hash: field-wise
        boolean hashedFields = record && !typeInfo.typeParameters().isEmpty() && fields.stream()
                .map(this::fieldType).anyMatch(t -> t.contains("HashSet<") || t.contains("HashMap<"));
        if (record) sb.append(hashedFields ? "#[derive(Debug, Clone)]\n" : "#[derive(Debug, Clone, PartialEq)]\n");
        else if (context.facts.immutable(typeInfo) || valueClass(typeInfo, fields)) sb.append("#[derive(Clone)]\n");
        String declaredGenerics = typestate ? "<" + RustTypestate.STATE + " = " + RustTypestate.BEFORE + ">" : generics;
        sb.append(visibility(typeInfo.isPrivate())).append("struct ").append(name).append(declaredGenerics).append(" {\n");
        if (this.base != null) sb.append(INDENT).append("pub base: ").append(this.base).append(",\n");
        for (FieldInfo f : fields) {
            sb.append(INDENT).append(record || !f.access().isPrivate() ? "pub " : "").append(RustNames.snake(f.name()))
                    .append(": ").append(fieldType(f)).append(",\n");
        }
        for (String tp : phantom) {
            context.use("std::marker::PhantomData");
            sb.append(INDENT).append("_").append(tp.toLowerCase()).append(": PhantomData<").append(tp).append(">,\n");
        }
        sb.append("}\n");

        // the inherent impl: constants, constructors, the methods no source trait declares
        StringBuilder inherent = new StringBuilder();
        for (FieldInfo f : typeInfo.fields()) {
            if (f.isStatic() && !f.isSynthetic()) inherent.append(staticField(f));
        }
        List<MethodInfo> constructors = typeInfo.constructors().stream()
                .filter(c -> !c.isSynthetic() || c.parameters().isEmpty()).toList();
        MethodInfo canonical = record ? typeInfo.constructors().stream()
                .filter(c -> c.parameters().size() == fields.size()).findFirst().orElse(null) : null;
        if (record && (canonical == null || canonical.isSynthetic())) {
            inherent.append(recordConstructor(canonical, fields, phantom));
        } else if (constructors.isEmpty()) {
            inherent.append(defaultConstructor(fields, phantom));
        }
        for (MethodInfo c : constructors) {
            if (c.isSynthetic() && record) continue;
            if (c.isSynthetic()) inherent.append(defaultConstructor(fields, phantom));
            else inherent.append(constructor(c, fields, phantom));
        }
        // a typestate: the methods of either state, and those of the after state; the inherent impl is the before state
        StringBuilder either = new StringBuilder();
        StringBuilder after = new StringBuilder();
        Map<TypeInfo, List<MethodInfo>> byTrait = new LinkedHashMap<>();
        Set<String> declared = typeInfo.methods().stream().filter(m -> !m.isSynthetic()).map(MethodInfo::name)
                .collect(Collectors.toSet());
        if (record) {
            for (FieldInfo f : fields) {
                if (!declared.contains(f.name())) inherent.append(accessor(f));
            }
        }
        MethodInfo equals = null;
        boolean hashCode = false;
        for (MethodInfo m : typeInfo.methods()) {
            if (m.isSynthetic()) continue;
            if (m.isStaticInitializer() || m.isInstanceInitializer()) {
                context.message(RustPrintCode.STATIC_STATE, m, m.isStaticInitializer() ? "static initializer"
                        : "instance initializer");
                continue;
            }
            if (isEquals(m)) {
                equals = m;
                continue;
            }
            hashCode |= isHashCode(m);
            TypeInfo trait = implementedTrait(m);
            if (trait != null) {
                byTrait.computeIfAbsent(trait, t -> new ArrayList<>()).add(m);
                // its own verdicts differ from the trait's: an inherent method with them, which the trait's delegates to
                MethodInfo declared2 = traitMethod(m, trait);
                if (declared2 != null && !shape(m).equals(shape(declared2))) {
                    inherent.append(method(m));
                }
            } else if (typestate) {
                switch (RustTypestate.side(m, context)) {
                    case ANY -> either.append(method(m));
                    case AFTER -> after.append(method(m));
                    case BEFORE -> inherent.append(method(m));
                    case MARK -> inherent.append(mark(m, name, fields, phantom));
                }
            } else {
                inherent.append(method(m));
            }
        }
        if (typestate) {
            sb.append("\nimpl ").append(name).append("<").append(RustTypestate.BEFORE).append("> {\n").append(inherent)
                    .append("}\n");
            if (!either.isEmpty()) {
                sb.append("\nimpl").append(implGenerics).append(" ").append(name).append(generics).append(" {\n")
                        .append(either).append("}\n");
            }
            if (!after.isEmpty()) {
                sb.append("\nimpl ").append(name).append("<").append(RustTypestate.AFTER).append("> {\n").append(after)
                        .append("}\n");
            }
        } else if (!inherent.isEmpty()) {
            sb.append("\nimpl").append(implGenerics).append(" ").append(name).append(generics).append(" {\n")
                    .append(inherent).append("}\n");
        }
        // unbounded: the struct holds any element, and Deref needs nothing of it (an element that is not Clone)
        if (base == null && baseType != null) sb.append(derefToBase(name, generics, generics, baseType));
        for (Map.Entry<TypeInfo, List<MethodInfo>> e : byTrait.entrySet()) {
            ParameterizedType traitType = typeInfo.interfacesImplemented().stream()
                    .filter(i -> i.typeInfo() == e.getKey()).findFirst().orElse(e.getKey().asSimpleParameterizedType());
            String trait = RustTypes.owned(traitType, context, typeInfo).replaceFirst("^Box<dyn (.*)>$", "$1");
            sb.append("\nimpl").append(implGenerics).append(" ").append(trait).append(" for ").append(name)
                    .append(generics).append(" {\n");
            for (MethodInfo m : e.getValue()) {
                MethodInfo declared2 = traitMethod(m, e.getKey());
                if (declared2 != null && !shape(m).equals(shape(declared2))) {
                    sb.append(delegate(m, declared2));
                } else {
                    sb.append(method(m, true));
                }
            }
            if (RustHierarchies.current().downcasts(e.getKey()).contains(typeInfo)) sb.append(conversions(typeInfo, true));
            sb.append("}\n");
        }
        if (equals != null) sb.append(partialEq(equals, name, generics, implGenerics));
        else if (hashedFields) {
            String eq = fields.stream().map(f -> "self." + RustNames.snake(f.name()) + " == other." + RustNames.snake(f.name()))
                    .collect(Collectors.joining(" && "));
            sb.append("\nimpl").append(implGenerics).append(" PartialEq for ").append(name).append(generics)
                    .append(" {\n").append(INDENT).append("fn eq(&self, other: &Self) -> bool {\n").append(INDENT)
                    .append(INDENT).append(eq.isEmpty() ? "true" : eq).append("\n").append(INDENT).append("}\n}\n");
        }
        if (hasToString(typeInfo)) {
            // Java's string conversion calls toString(): Rust's Display, through the inherent to_string
            sb.append("\nimpl").append(implGenerics).append(" std::fmt::Display for ").append(name).append(generics)
                    .append(" {\n").append(INDENT)
                    .append("fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {\n")
                    .append(INDENT).append(INDENT).append("write!(f, \"{}\", ").append(name).append("::to_string(self))\n")
                    .append(INDENT).append("}\n}\n");
        }
        if (hashCode) {
            if (equals != null) sb.append("\nimpl").append(implGenerics).append(" Eq for ").append(name).append(generics)
                    .append(" {}\n");
            // a hashCode that caches its value (VarVersion's) modifies: Hash's &self hashes a copy, which Java's equals
            // cannot tell from the object
            MethodInfo hc = typeInfo.methods().stream().filter(RustTypePrinter::isHashCode).findFirst().orElse(null);
            boolean caching = hc != null && context.facts.receiver(hc) == Receiver.EXCLUSIVE
                              && (context.facts.immutable(typeInfo) || valueClass(typeInfo, fields));
            sb.append("\nimpl").append(implGenerics).append(" std::hash::Hash for ").append(name).append(generics)
                    .append(" {\n").append(INDENT).append("fn hash<H: std::hash::Hasher>(&self, state: &mut H) {\n")
                    .append(INDENT).append(INDENT)
                    .append(caching ? "state.write_i32(self.clone().hash_code());\n" : "state.write_i32(self.hash_code());\n")
                    .append(INDENT)
                    .append("}\n}\n");
        }
        return sb.toString();
    }

    /**
     * A trait's conversions of its value to {@code struct} ({@link RustHierarchies#downcasts}): {@code None} in the
     * trait, {@code Some(self)} in the struct's impl. The exclusive one only for a struct without a lifetime, a
     * {@code &mut} being invariant in it.
     */
    private String conversions(TypeInfo struct, boolean impl) {
        String name = RustHierarchies.downcastName(struct);
        boolean lifetime = context.borrows.hasLifetime(struct);
        String type = RustNames.ownType(struct) + (lifetime ? "<'_>" : "");
        String module = RustNames.module(struct.packageName());
        if (!module.equals(context.module)) context.use("crate::" + module + "::" + RustNames.ownType(struct));
        String value = impl ? "Some(self)" : "None";
        StringBuilder sb = new StringBuilder(INDENT).append("fn ").append(name).append("(&self) -> Option<&")
                .append(type).append("> {\n").append(INDENT).append(INDENT).append(value).append("\n").append(INDENT)
                .append("}\n");
        if (!lifetime) {
            sb.append(INDENT).append("fn ").append(name).append("_mut(&mut self) -> Option<&mut ").append(type)
                    .append("> {\n").append(INDENT).append(INDENT).append(value).append("\n").append(INDENT).append("}\n");
        }
        return sb.toString();
    }

    // ---- a closed hierarchy (RustHierarchies)

    /**
     * The root of a closed hierarchy and all its members: the root's struct with the root's fields and the
     * {@code kind}; the kind's enum, a variant per member, carrying the data structs of the member's chain below the
     * root; those data structs; the kind's accessors of each data; and one impl with every member's constructors,
     * constants and methods, an overridden method's family dispatched on the kind.
     */
    private String hierarchyType(TypeInfo root) {
        RustHierarchies hs = RustHierarchies.current();
        RustHierarchies.Hierarchy h = hs.hierarchy(root);
        context.message(RustPrintCode.HIERARCHY_AS_ENUM, root, root.fullyQualifiedName());
        String name = RustNames.ownType(root);
        String kind = hs.kind(root);
        boolean lifetime = context.borrows.hasLifetime(root);
        String generics = lifetime ? "<" + RustBorrows.LIFETIME + ">" : "";
        boolean kindLifetime = h.members().stream().anyMatch(m -> m != root && dataLifetime(m));
        String kindGenerics = kindLifetime ? "<" + RustBorrows.LIFETIME + ">" : "";

        StringBuilder sb = new StringBuilder(doc(root));
        sb.append(visibility(root.isPrivate())).append("struct ").append(name).append(generics).append(" {\n");
        for (FieldInfo f : instanceFields(root)) {
            sb.append(INDENT).append(f.access().isPrivate() ? "" : "pub ").append(RustNames.snake(f.name()))
                    .append(": ").append(fieldType(f)).append(",\n");
        }
        sb.append(INDENT).append("pub kind: ").append(kind).append(kindGenerics).append(",\n}\n");

        sb.append("\n/// The class of an object of `").append(root.fullyQualifiedName())
                .append("`, with the fields each class below it adds.\npub enum ").append(kind).append(kindGenerics)
                .append(" {\n");
        for (TypeInfo m : h.members()) {
            sb.append(INDENT).append(RustHierarchies.variant(m));
            if (m != root) {
                sb.append("(").append(hs.chain(m).stream().map(this::dataType).collect(Collectors.joining(", ")))
                        .append(")");
            }
            sb.append(",\n");
        }
        sb.append("}\n");

        TypeInfo outer = context.type;
        try {
            for (TypeInfo m : h.members()) {
                if (m == root) continue;
                context.type = m;
                sb.append("\n/// The fields `").append(m.fullyQualifiedName()).append("` adds.\npub struct ")
                        .append(dataType(m)).append(" {\n");
                for (FieldInfo f : instanceFields(m)) {
                    sb.append(INDENT).append("pub ").append(RustNames.snake(f.name())).append(": ")
                            .append(fieldType(f)).append(",\n");
                }
                sb.append("}\n");
            }
        } finally {
            context.type = outer;
        }

        sb.append(kindAccessors(h, kind, kindGenerics));

        StringBuilder inherent = new StringBuilder();
        StringBuilder traits = new StringBuilder();
        try {
            for (TypeInfo m : h.members()) {
                context.type = m;
                for (FieldInfo f : m.fields()) {
                    if (f.isStatic() && !f.isSynthetic()) inherent.append(staticField(f));
                }
                inherent.append(hierarchyConstructors(h, m));
                for (MethodInfo method : m.methods()) {
                    if (method.isSynthetic()) continue;
                    if (method.isStaticInitializer() || method.isInstanceInitializer()) {
                        context.message(RustPrintCode.STATIC_STATE, method, method.isStaticInitializer()
                                ? "static initializer" : "instance initializer");
                        continue;
                    }
                    if (isEquals(method) || isHashCode(method)) {
                        context.message(RustPrintCode.UNSUPPORTED, method, "equals or hashCode in a class hierarchy");
                        continue;
                    }
                    RustHierarchies.Family family = hs.family(method);
                    if (family.dispatched() && (method.isAbstract() || method.methodBody() == null)) continue;
                    methodName = hs.implementationName(method);
                    try {
                        inherent.append(method(method));
                    } finally {
                        methodName = null;
                    }
                }
            }
            context.type = root;
            Set<RustHierarchies.Family> done = new HashSet<>();
            for (TypeInfo m : h.members()) {
                for (MethodInfo method : m.methods()) {
                    RustHierarchies.Family family = hs.family(method);
                    if (family == null || !family.dispatched() || !done.add(family)) continue;
                    if (isEquals(method) || isHashCode(method)) continue;
                    inherent.append(dispatcher(h, family));
                }
            }
            traits.append(hierarchyTraits(h, name, generics));
        } finally {
            context.type = outer;
        }
        sb.append("\nimpl").append(generics).append(" ").append(name).append(generics).append(" {\n").append(inherent)
                .append("}\n").append(traits);
        if (h.members().stream().anyMatch(RustTypePrinter::hasToString)) {
            sb.append("\nimpl").append(generics).append(" std::fmt::Display for ").append(name).append(generics)
                    .append(" {\n").append(INDENT)
                    .append("fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {\n")
                    .append(INDENT).append(INDENT).append("write!(f, \"{}\", ").append(name)
                    .append("::to_string(self))\n").append(INDENT).append("}\n}\n");
        }
        return sb.toString();
    }

    private static List<FieldInfo> instanceFields(TypeInfo t) {
        return t.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).toList();
    }

    /** A member's data struct carries the lifetime when one of its fields does. */
    private boolean dataLifetime(TypeInfo member) {
        return context.borrows.fieldsMentionLifetime(member);
    }

    private String dataType(TypeInfo member) {
        return RustHierarchies.data(member) + (dataLifetime(member) ? "<" + RustBorrows.LIFETIME + ">" : "");
    }

    /** {@code fn assignment_exprent(&self) -> &AssignmentExprentData}, and its {@code _mut} twin, on the kind. */
    private String kindAccessors(RustHierarchies.Hierarchy h, String kind, String kindGenerics) {
        RustHierarchies hs = RustHierarchies.current();
        StringBuilder sb = new StringBuilder();
        for (TypeInfo k : h.members()) {
            if (k == h.root()) continue;
            for (boolean exclusive : new boolean[]{false, true}) {
                sb.append(INDENT).append("pub fn ").append(RustHierarchies.accessor(k)).append(exclusive ? "_mut" : "")
                        .append("(").append(exclusive ? "&mut self" : "&self").append(") -> ")
                        .append(exclusive ? "&mut " : "&").append(dataType(k)).append(" {\n")
                        .append(INDENT).append(INDENT).append("match self {\n");
                for (TypeInfo d : hs.descendantsOrSelf(k)) {
                    List<TypeInfo> chain = hs.chain(d);
                    int at = chain.indexOf(k);
                    List<String> parts = new ArrayList<>();
                    for (int i = 0; i < chain.size(); i++) parts.add(i == at ? "data" : "_");
                    sb.append(INDENT.repeat(3)).append(kind).append("::").append(RustHierarchies.variant(d))
                            .append("(").append(String.join(", ", parts)).append(") => data,\n");
                }
                sb.append(INDENT.repeat(3)).append("_ => panic!(\"ClassCastException: not a ")
                        .append(k.simpleName()).append("\"),\n")
                        .append(INDENT).append(INDENT).append("}\n").append(INDENT).append("}\n");
            }
        }
        if (sb.isEmpty()) return "";
        return "\nimpl" + kindGenerics + " " + kind + kindGenerics + " {\n" + sb + "}\n";
    }

    /*
     A member's constructors, associated functions of the root. The root's build the struct with its kind at the
     root's variant; a member's below it start from the parent's constructor its super(...) names, move the parent's
     data into the member's variant with the member's fields at their initial values, then run the body.
     */
    private String hierarchyConstructors(RustHierarchies.Hierarchy h, TypeInfo m) {
        RustHierarchies hs = RustHierarchies.current();
        List<MethodInfo> constructors = m.constructors().stream().filter(c -> !c.isSynthetic()).toList();
        StringBuilder sb = new StringBuilder();
        if (m == h.root()) {
            kindInit = hs.kind(m) + "::" + RustHierarchies.variant(m);
            try {
                List<FieldInfo> fields = instanceFields(m);
                if (constructors.isEmpty()) {
                    sb.append(defaultConstructor(RustHierarchies.defaultConstructorName(m), fields, List.of()));
                }
                for (MethodInfo c : constructors) sb.append(constructor(c, fields, List.of()));
            } finally {
                kindInit = null;
            }
            return sb.toString();
        }
        if (constructors.isEmpty()) {
            sb.append(INDENT).append("pub fn ").append(RustHierarchies.defaultConstructorName(m)).append("() -> Self {\n")
                    .append(INDENT).append(INDENT).append("let mut this = Self::").append(parentNoArgConstructor(m))
                    .append("();\n").append(intoVariant(m, INDENT.repeat(2), Map.of(), null))
                    .append(INDENT).append(INDENT).append("this\n").append(INDENT).append("}\n");
        }
        for (MethodInfo c : constructors) sb.append(subConstructor(m, c));
        return sb.toString();
    }

    private static String parentNoArgConstructor(TypeInfo m) {
        TypeInfo parent = m.parentClass().typeInfo();
        return parent.constructors().stream().filter(c -> !c.isSynthetic() && c.parameters().isEmpty()).findFirst()
                .map(RustHierarchies::constructorName)
                .orElse(RustHierarchies.defaultConstructorName(parent));
    }

    /** {@code this.kind = Kind::M(parent's data…, MData { … });}: the object becomes one of {@code m}. */
    private String intoVariant(TypeInfo m, String pad, Map<FieldInfo, Expression> assigned, RustBodyPrinter printer) {
        RustHierarchies hs = RustHierarchies.current();
        TypeInfo parent = m.parentClass().typeInfo();
        String kind = hs.kind(m);
        StringBuilder sb = new StringBuilder();
        List<String> parts = new ArrayList<>();
        if (hs.isSubMember(parent)) {
            int n = hs.chain(parent).size();
            for (int i = 0; i < n; i++) parts.add("d" + i);
            sb.append(pad).append("let ").append(kind).append("::").append(RustHierarchies.variant(parent)).append("(")
                    .append(String.join(", ", parts)).append(") = std::mem::replace(&mut this.kind, ")
                    .append(kind).append("::").append(RustHierarchies.variant(hs.root(m)))
                    .append(") else { unreachable!() };\n");
        }
        StringBuilder data = new StringBuilder(RustHierarchies.data(m)).append(" {");
        TypeInfo outer = context.type;
        context.type = m;
        try {
            List<FieldInfo> fields = instanceFields(m);
            for (int i = 0; i < fields.size(); i++) {
                FieldInfo f = fields.get(i);
                Expression value = assigned.get(f);
                data.append(i == 0 ? " " : ", ").append(RustNames.snake(f.name())).append(": ")
                        .append(value == null ? initial(f) : fieldValue(printer, f, value));
            }
        } finally {
            context.type = outer;
        }
        parts.add(data.append(" }").toString());
        return sb.append(pad).append("this.kind = ").append(kind).append("::").append(RustHierarchies.variant(m))
                .append("(").append(String.join(", ", parts)).append(");\n").toString();
    }

    private String subConstructor(TypeInfo m, MethodInfo c) {
        context.method = c;
        String outerReceiver = context.receiver;
        try {
            StringBuilder sb = new StringBuilder(INDENT + visibility(c.access().isPrivate()) + "fn "
                                                 + RustNames.method(c) + "(" + parameters(c) + ") -> Self {\n");
            String pad = INDENT.repeat(2);
            List<Statement> statements = c.methodBody() == null ? List.of() : c.methodBody().statements();
            RustBodyPrinter printer = new RustBodyPrinter(context);
            ExplicitConstructorInvocation eci = statements.stream()
                    .filter(s -> s instanceof ExplicitConstructorInvocation).map(s -> (ExplicitConstructorInvocation) s)
                    .findFirst().orElse(null);
            // fields the body assigns from what does not read 'this': their values go into the variant's data, the
            // other statements first, as a root's literal has it; otherwise the data at the initial values
            List<FieldInfo> fields = instanceFields(m);
            Map<FieldInfo, Expression> assigned = eci != null && !eci.isSuper() ? null
                    : simpleAssignments(statements, fields);
            if (assigned != null) {
                for (Statement s : statements) {
                    if (!(s instanceof ExplicitConstructorInvocation) && assignedField(s, fields) == null) {
                        sb.append(printer.statement(s, 2));
                    }
                }
            }
            if (eci != null && !eci.isSuper()) {
                // this(...): the other constructor makes it one of m already
                sb.append(pad).append("let mut this = Self::").append(RustNames.method(eci.methodInfo())).append("(")
                        .append(printer.arguments(eci.parameterExpressions(), eci.methodInfo())).append(");\n");
            } else {
                String parent = eci == null ? parentNoArgConstructor(m) : RustNames.method(eci.methodInfo());
                String args = eci == null ? "" : printer.arguments(eci.parameterExpressions(), eci.methodInfo());
                sb.append(pad).append("let mut this = Self::").append(parent).append("(").append(args).append(");\n");
                sb.append(intoVariant(m, pad, assigned == null ? Map.of() : assigned, printer));
            }
            context.receiver = "this";
            if (assigned == null) {
                for (Statement s : statements) {
                    if (s instanceof ExplicitConstructorInvocation) continue;
                    sb.append(printer.statement(s, 2));
                }
            }
            return sb.append(pad).append("this\n").append(INDENT).append("}\n").toString();
        } finally {
            context.receiver = outerReceiver;
            context.method = null;
        }
    }

    /**
     * The method of a family's name: a match on the kind, each variant calling the implementation of its own class or
     * the nearest one above it. Its signature is the family's top method's, with its verdicts: an implementation
     * that modifies where the top does not is rustc's error in the arm that calls it.
     */
    private String dispatcher(RustHierarchies.Hierarchy h, RustHierarchies.Family family) {
        RustHierarchies hs = RustHierarchies.current();
        MethodInfo representative = family.representative();
        if (representative.isStatic() || context.facts.receiver(representative) == Receiver.NONE) return "";
        methodName = family.name();
        String signature;
        try {
            signature = signature(representative, true).replaceAll("\\bmut ((r#)?\\w+): ", "$1: ");
        } finally {
            methodName = null;
        }
        String args = representative.parameters().stream().map(p -> RustNames.snake(p.name()))
                .collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder(INDENT).append(signature).append(" {\n").append(INDENT).append(INDENT)
                .append("match self.kind {\n");
        for (TypeInfo member : h.members()) {
            MethodInfo impl = null;
            for (TypeInfo c = member; c != null && impl == null; c = hs.isMember(c) && c != h.root()
                    ? c.parentClass().typeInfo() : null) {
                TypeInfo at = c;
                impl = family.methods().stream().filter(x -> x.typeInfo() == at && !x.isAbstract()
                                                             && x.methodBody() != null).findFirst().orElse(null);
            }
            sb.append(INDENT.repeat(3)).append(hs.pattern(member)).append(" => ");
            if (impl == null) {
                sb.append("unreachable!(\"abstract\"),\n");
            } else {
                sb.append("self.").append(hs.implementationName(impl)).append("(").append(args).append("),\n");
            }
        }
        return sb.append(INDENT).append(INDENT).append("}\n").append(INDENT).append("}\n").toString();
    }

    /** The source interfaces of the hierarchy's members, each implemented on the root through the families. */
    private String hierarchyTraits(RustHierarchies.Hierarchy h, String name, String generics) {
        Set<TypeInfo> interfaces = new java.util.LinkedHashSet<>();
        for (TypeInfo m : h.members()) superInterfaces(m, interfaces);
        StringBuilder sb = new StringBuilder();
        for (TypeInfo i : interfaces) {
            if (i.compilationUnit().externalLibrary()) continue;
            String trait = RustTypes.owned(i.asSimpleParameterizedType(), context, h.root())
                    .replaceFirst("^Box<dyn (.*)>$", "$1");
            sb.append("\nimpl").append(generics).append(" ").append(trait).append(" for ").append(name).append(generics)
                    .append(" {\n");
            for (MethodInfo d : i.methods()) {
                if (d.isSynthetic() || d.isStatic()) continue;
                MethodInfo impl = h.members().stream().flatMap(m -> m.methods().stream())
                        .filter(m -> m.overrides().contains(d)).findFirst().orElse(null);
                if (impl == null) continue;
                sb.append(delegate(impl, d));
            }
            if (RustHierarchies.current().downcasts(i).contains(h.root())) sb.append(conversions(h.root(), true));
            sb.append("}\n");
        }
        return sb.toString();
    }

    /** {@code boolean equals(Object)}: Rust's {@code PartialEq}, whose other side is already of this type. */
    static boolean isEquals(MethodInfo m) {
        return "equals".equals(m.name()) && !m.isStatic() && m.parameters().size() == 1
               && m.parameters().getFirst().parameterizedType().typeInfo() != null
               && m.parameters().getFirst().parameterizedType().typeInfo().isJavaLangObject()
               && m.returnType().isBoolean();
    }

    /** The type declares {@code String toString()}. */
    static boolean hasToString(TypeInfo typeInfo) {
        // not a record's synthetic one: Display would call itself through ToString
        return typeInfo.methods().stream().anyMatch(m -> "toString".equals(m.name()) && !m.isStatic() && !m.isSynthetic()
                                                         && m.parameters().isEmpty() && !m.isAbstract());
    }

    private static boolean isHashCode(MethodInfo m) {
        return "hashCode".equals(m.name()) && !m.isStatic() && m.parameters().isEmpty() && m.returnType().isInt();
    }

    private String partialEq(MethodInfo equals, String name, String generics, String implGenerics) {
        context.method = equals;
        try {
            String body = new RustBodyPrinter(context).equalsBody(equals, 2);
            return "\nimpl" + implGenerics + " PartialEq for " + name + generics + " {\n" + INDENT
                   + "fn eq(&self, other: &Self) -> bool {\n" + body + INDENT + "}\n}\n";
        } finally {
            context.method = null;
        }
    }

    /** The parent's state in the subclass's literal: the {@code super(…)} call, or the parent's no-argument one. */
    private String baseValue(List<Statement> statements, RustBodyPrinter printer) {
        String owner = base.replaceFirst("<.*$", "");
        if (COLLECTION_BASES.contains(owner)) return owner + "::new()";
        if (statements != null) {
            for (Statement s : statements) {
                if (s instanceof ExplicitConstructorInvocation eci && eci.isSuper() && STREAM_BASE.equals(owner)) {
                    // super(new ByteArrayInputStream(bytes)): the stream over those bytes; nothing else is in memory
                    if (eci.parameterExpressions().size() == 1
                        && eci.parameterExpressions().getFirst() instanceof ConstructorCall cc
                        && cc.constructor() != null
                        && "java.io.ByteArrayInputStream".equals(cc.constructor().typeInfo().fullyQualifiedName())
                        && cc.parameterExpressions().size() == 1) {
                        return owner + "::from_bytes(" + printer.arguments(cc.parameterExpressions(), cc.constructor())
                               + ")";
                    }
                    return owner + "::default()";
                }
                if (s instanceof ExplicitConstructorInvocation eci && eci.isSuper() && eci.methodInfo() != null) {
                    return owner + "::" + RustNames.method(eci.methodInfo()) + "("
                           + printer.arguments(eci.parameterExpressions(), eci.methodInfo()) + ")";
                }
            }
        }
        return owner + "::new()";
    }

    /**
     * The superclass, when it is a JDK collection that Rust has as a standard collection ({@code class Named<E> extends
     * ArrayList<E>}): its Rust type ({@code Vec<E>}), the {@code base} field the struct dereferences to, so that what
     * the class inherits ({@code size()}, {@code get(i)}, for-each) is the collection's; {@code null} otherwise.
     */
    static String collectionBase(TypeInfo typeInfo, RustContext context) {
        ParameterizedType parent = typeInfo.parentClass();
        if (parent == null || parent.typeInfo() == null || parent.typeInfo().isJavaLangObject()
            || !parent.typeInfo().compilationUnit().externalLibrary()) {
            return null;
        }
        String owned = RustTypes.owned(parent, context, typeInfo);
        String owner = owned.replaceFirst("<.*$", "");
        return COLLECTION_BASES.contains(owner) || STREAM_BASE.equals(owner) ? owned : null;
    }

    /** jrt.rs's in-memory stream, the base of a class extending {@code DataInputStream} (fernflower's own stream). */
    private static final String STREAM_BASE = "crate::jrt::DataInputStream";

    private static final Set<String> COLLECTION_BASES = Set.of("Vec", "VecDeque", "HashMap", "HashSet", "BTreeMap",
            "BTreeSet");

    /** {@code Deref} and {@code DerefMut} to the {@link #collectionBase}. */
    private static String derefToBase(String name, String generics, String implGenerics, String baseType) {
        String self = name + generics;
        return "\nimpl" + implGenerics + " std::ops::Deref for " + self + " {\n" + INDENT + "type Target = " + baseType
               + ";\n" + INDENT + "fn deref(&self) -> &Self::Target {\n" + INDENT + INDENT + "&self.base\n" + INDENT
               + "}\n}\n\nimpl" + implGenerics + " std::ops::DerefMut for " + self + " {\n" + INDENT
               + "fn deref_mut(&mut self) -> &mut Self::Target {\n" + INDENT + INDENT + "&mut self.base\n" + INDENT
               + "}\n}\n";
    }

    /** The superclass, when it is a source class: its state becomes a {@code base} field. */
    static TypeInfo sourceParent(TypeInfo typeInfo) {
        ParameterizedType parent = typeInfo.parentClass();
        if (parent == null || parent.typeInfo() == null) return null;
        TypeInfo p = parent.typeInfo();
        if (p.isJavaLangObject() || p.compilationUnit().externalLibrary() || p.isInterface()) return null;
        // a closed hierarchy is one struct (RustHierarchies), not composition
        if (RustHierarchies.current().isMember(typeInfo)) return null;
        return p;
    }

    /** {@code .base.base}: the path from an object of {@code from} to the part that {@code owner} declares. */
    static String basePath(TypeInfo from, TypeInfo owner) {
        StringBuilder sb = new StringBuilder();
        TypeInfo t = from;
        while (t != null && t != owner) {
            t = sourceParent(t);
            sb.append(".base");
        }
        return t == owner ? sb.toString() : "";
    }

    /** The signature without the parameters' names, which may differ between the trait and the impl. */
    private String shape(MethodInfo m) {
        return signature(m, false).replaceAll("(mut )?r?#?[a-z_][a-z0-9_]*: ", "");
    }

    /** The method of {@code trait} that {@code m} implements. */
    private static MethodInfo traitMethod(MethodInfo m, TypeInfo trait) {
        return m.overrides().stream().filter(o -> o.typeInfo() == trait).findFirst().orElse(null);
    }

    /**
     * The trait's method, with the trait's receiver and parameters, calling the inherent method that has the
     * implementation's own: the interface's verdict is the union over its implementations (or unknown, so
     * modifying), and Rust requires the impl to repeat the trait's signature. A borrowed parameter of the
     * implementation is borrowed from the trait's owned one.
     */
    private String delegate(MethodInfo m, MethodInfo declared) {
        List<String> args = new ArrayList<>();
        for (int i = 0; i < m.parameters().size(); i++) {
            ParameterInfo own = m.parameters().get(i);
            ParameterInfo theirs = declared.parameters().get(i);
            String name = RustNames.snake(theirs.name());
            boolean copy = RustTypes.isCopy(own.parameterizedType());
            Passing want = context.passing(own);
            Passing have = context.passing(theirs);
            if (!copy && have == Passing.OWNED && want == Passing.SHARED) name = "&" + name;
            else if (!copy && have == Passing.OWNED && want == Passing.EXCLUSIVE) name = "&mut " + name;
            args.add(name);
        }
        boolean self = context.facts.receiver(m) != Receiver.NONE;
        String call = "Self::" + RustNames.method(m) + "(" + (self ? "self" + (args.isEmpty() ? "" : ", ") : "")
                      + String.join(", ", args) + ")";
        return INDENT + signature(declared, false) + " {\n" + INDENT + INDENT + call + "\n" + INDENT + "}\n";
    }

    /** A source interface the method implements, directly or through an interface it extends. */
    static TypeInfo implementedTrait(MethodInfo m) {
        Set<TypeInfo> traits = new HashSet<>();
        superInterfaces(m.typeInfo(), traits);
        for (MethodInfo o : m.overrides()) {
            TypeInfo t = o.typeInfo();
            if (t.isInterface() && !t.compilationUnit().externalLibrary() && traits.contains(t)) return t;
        }
        return null;
    }

    /** The interfaces a type implements, and those they extend: Rust wants an impl for each supertrait too. */
    private static void superInterfaces(TypeInfo typeInfo, Set<TypeInfo> into) {
        for (ParameterizedType i : typeInfo.interfacesImplemented()) {
            if (i.typeInfo() != null && into.add(i.typeInfo())) superInterfaces(i.typeInfo(), into);
        }
    }

    /** Rust rejects a type parameter no field uses: those get a {@code PhantomData} field. */
    private static List<String> phantomTypeParameters(TypeInfo typeInfo, List<FieldInfo> fields) {
        return typeInfo.typeParameters().stream()
                .filter(tp -> fields.stream().noneMatch(f -> CstQueries.mentions(f.type(), tp)))
                .map(TypeParameter::simpleName).toList();
    }

    private String fieldType(FieldInfo f) {
        if (context.cells.isCell(f)) {
            context.use("std::cell::RefCell");
            return "RefCell<" + RustTypes.option(RustTypes.owned(f.type(), context, f),
                    RustFacts.nullable(context.facts.field(f))) + ">";
        }
        RustTypes.Form borrowed = context.borrows.fields(f.owner()).get(f);
        if (borrowed != null) {
            String type = RustTypes.type(f.type(), borrowed, context, f).replaceFirst("^&", "&" + RustBorrows.LIFETIME + " ");
            return RustTypes.option(type, RustFacts.nullable(context.facts.field(f)));
        }
        String owned = RustTypes.owned(f.type(), context, f);
        if (context.boxes.isBoxed(f)) owned = "Box<" + owned + ">";
        return RustTypes.option(owned, RustFacts.nullable(context.facts.field(f)));
    }

    /** The module-level items a type's static state becomes: printed after the type's own items. */
    private final StringBuilder statics = new StringBuilder();

    /**
     * A {@code static final} field that is not a constant, whose object no method modifies, and that no static
     * initializer touches: a module-level {@code static}, built on first use ({@code LazyLock}), shared read-only. Its
     * type must be shareable between threads ({@code Sync}): not a {@code dyn} value.
     */
    /*
     A class with its own equals over primitives and strings (fernflower's VarVersion, whose cached hashCode keeps it
     from being immutable): Java's equals cannot tell a copy from the object, so it is Clone, and a collection of them
     can be copied (extend(other.iter().cloned())).
     */
    private boolean valueClass(TypeInfo typeInfo, List<FieldInfo> fields) {
        return base == null && !fields.isEmpty() && !RustHierarchies.current().isMember(typeInfo)
               && typeInfo.methods().stream().anyMatch(RustTypePrinter::isEquals)
               && fields.stream().allMatch(f -> RustTypes.isCopy(f.type()) || RustTypes.isString(f.type()));
    }

    /*
     The static's object is not modified: by the verdict, or by its type, immutable with hidden content or without. Its
     own methods do not change it, and its hidden content is reached through a shared borrow only: fernflower's
     FieldDescriptor.INTEGER_DESCRIPTOR = parseDescriptor(…), modified by the verdict through a VarType that may be a
     GenericType (maddi-mod#101, a factory rather than a constructor). A LazyLock or an Rc hands out shared borrows
     only: a modification would be rustc's error, not a wrong translation.
     */
    private static boolean readOnly(FieldInfo f, RustContext context) {
        if (context.facts.unmodified(f)) return true;
        TypeInfo ti = f.type().arrays() == 0 ? f.type().typeInfo() : null;
        return ti != null && !ti.compilationUnit().externalLibrary()
               && (context.facts.immutable(ti) || context.facts.immutableHc(ti));
    }

    static boolean lazyStatic(FieldInfo f, RustContext context) {
        if (!f.isStatic() || !isFinal(f) || f.initializer() == null || f.initializer().isEmpty()
            || constant(f) || !readOnly(f, context)) {
            return false;
        }
        String type = RustTypes.owned(f.type(), context, f);
        if (type.contains("dyn ") || type.contains("RefCell<")) return false;
        boolean[] touched = {false};
        for (MethodInfo m : f.owner().methods()) {
            if (!m.isStaticInitializer() || m.methodBody() == null) continue;
            m.methodBody().visit(e -> {
                if (e instanceof VariableExpression ve
                    && ve.variable() instanceof FieldReference fr
                    && fr.fieldInfo() == f) {
                    touched[0] = true;
                }
                return !touched[0];
            });
        }
        return !touched[0];
    }

    /**
     * The initial value of a static field that is Java's mutable static state, a module-level {@code static mut}: not
     * final, of a type whose empty value Rust can build at compile time (a primitive, {@code String}, {@code Vec},
     * {@code VecDeque}, {@code BTreeMap}, {@code BTreeSet}), initialized with a constant or not at all; or a final
     * collection of those whose object is modified ({@code static final List<String> ALL = new ArrayList<>()}).
     * {@code null} for any other static field.
     */
    static String mutableStaticValue(FieldInfo f, RustContext context) {
        if (!f.isStatic() || f.owner().compilationUnit().externalLibrary()) return null;
        if (isFinal(f) && (f.type().isPrimitiveExcludingVoid() || RustTypes.isString(f.type())
                            || context.facts.unmodified(f))) {
            return null;
        }
        Expression init = f.initializer();
        boolean empty = init == null || init instanceof EmptyExpression
                        || init instanceof ConstructorCall cc && cc.parameterExpressions().isEmpty();
        if (f.type().isPrimitiveExcludingVoid() && f.type().arrays() == 0) {
            if (empty) return RustTypes.empty(f.type(), false, context, f);
            return init instanceof ConstantExpression<?> ? new RustBodyPrinter(context).text(init) : null;
        }
        if (!empty) return null;
        String owned = RustTypes.owned(f.type(), context, f);
        String base = owned.replaceFirst("<.*$", "");
        return Set.of("String", "Vec", "VecDeque", "BTreeMap", "BTreeSet").contains(base) ? base + "::new()"
                : null;
    }

    /**
     * A method that touches a {@code static mut}, of any type, or calls one that does, or has static side effects by
     * its verdict ({@link RustUnsafety}): an {@code unsafe fn}, its body an {@code unsafe} block. A call of it from a
     * method that is not is rustc's E0133.
     */
    static boolean unsafeMethod(MethodInfo m, RustContext context) {
        return context.unsafety.unsafe(m, context);
    }

    /** Final as Java has it: also a field of an interface, implicitly {@code static final}. */
    static boolean isFinal(FieldInfo f) {
        return f.isFinal() || f.owner().isInterface();
    }

    /**
     * A constant of an interface ({@code int TYPE_OBJECT = 8;}): a module-level {@code const}, named like a lazy
     * static ({@link #staticName}), as a trait's associated const cannot be named without a type.
     */
    static boolean interfaceConstant(FieldInfo f) {
        return f.owner().isInterface() && constant(f);
    }

    /**
     * A {@link #lazyStatic} field held in an {@code Rc} or a {@code Shared} (VarType's {@code VARTYPE_INT}): not
     * {@code Sync}, so a {@code thread_local!}, read as {@code NAME.with(|v| v.clone())}, a handle on the one object.
     */
    static boolean threadLocalStatic(FieldInfo f, RustContext context) {
        String type = RustTypes.owned(f.type(), context, f);
        return type.contains("Rc<") || type.contains("Shared<");
    }

    /** The module-level name of a {@link #lazyStatic} field: its owner's name and its own. */
    static String staticName(FieldInfo f) {
        return RustNames.constant(RustNames.ownType(f.owner()) + "_" + f.name());
    }

    /**
     * The associated {@code const} of a constant: its own name; a member's below the root of a closed hierarchy, whose
     * constants are the root's, with its class's in front ({@code ASSIGNMENT_EXPRENT_MAX}).
     */
    static String constantName(FieldInfo f) {
        if (!RustHierarchies.current().isSubMember(f.owner())) return RustNames.constant(f.name());
        return RustNames.constant(RustNames.snake(RustNames.ownType(f.owner())) + "_" + f.name());
    }

    /** A {@code static final} primitive or string with a literal value: an associated {@code const}. */
    static boolean constant(FieldInfo f) {
        return isFinal(f) && f.initializer() instanceof ConstantExpression<?>
               && (f.type().isPrimitiveExcludingVoid() || RustTypes.isString(f.type()));
    }

    private String staticField(FieldInfo f) {
        Expression init = f.initializer();
        boolean constant = constant(f);
        if (!constant && lazyStatic(f, context)) {
            String type = RustTypes.owned(f.type(), context, f);
            // module level: no Self, the owner by its name
            String value = new RustBodyPrinter(context).ownedAs(init, f.type())
                    .replaceAll("\\bSelf::", java.util.regex.Matcher.quoteReplacement(RustNames.type(f.owner()) + "::"));
            if (threadLocalStatic(f, context)) {
                statics.append("\nthread_local! {\n    /// Translated from the static field `")
                        .append(f.owner().fullyQualifiedName()).append(".").append(f.name())
                        .append("`: built on first use, read-only, one per thread (an Rc is not Sync).\n    ")
                        .append(visibility(f.access().isPrivate())).append("static ").append(staticName(f))
                        .append(": ").append(type).append(" = ").append(value).append(";\n}\n");
                return "";
            }
            statics.append("\n/// Translated from the static field `").append(f.owner().fullyQualifiedName()).append(".")
                    .append(f.name()).append("`: built on first use, read-only.\n")
                    .append(visibility(f.access().isPrivate())).append("static ").append(staticName(f))
                    .append(": std::sync::LazyLock<").append(type).append("> = std::sync::LazyLock::new(|| ")
                    .append(value).append(");\n");
            return "";
        }
        String mutable = constant ? null : mutableStaticValue(f, context);
        if (mutable != null) {
            statics.append("\n/// Translated from the static field `").append(f.owner().fullyQualifiedName()).append(".")
                    .append(f.name()).append("`: Java's static state, `unsafe` to touch.\n")
                    .append(visibility(f.access().isPrivate())).append("static mut ").append(staticName(f)).append(": ").append(RustTypes.owned(f.type(), context, f))
                    .append(" = ").append(mutable).append(";\n");
            return "";
        }
        if (!constant) {
            context.message(RustPrintCode.STATIC_STATE, f, f.name());
            return INDENT + "// static " + f.name() + ": not translated\n";
        }
        String type = RustTypes.isString(f.type()) ? "&'static str" : RustTypes.owned(f.type(), context, f);
        String value = new RustBodyPrinter(context).text(init);
        if (f.owner().isInterface()) {
            statics.append("\n/// Translated from the constant `").append(f.owner().fullyQualifiedName()).append(".")
                    .append(f.name()).append("`.\npub const ").append(staticName(f)).append(": ").append(type)
                    .append(" = ").append(value).append(";\n");
            return "";
        }
        return INDENT + visibility(f.access().isPrivate()) + "const " + constantName(f) + ": " + type + " = "
               + value + ";\n";
    }

    // ---- constructors

    private String defaultConstructor(List<FieldInfo> fields, List<String> phantom) {
        return defaultConstructor("new", fields, phantom);
    }

    private String defaultConstructor(String name, List<FieldInfo> fields, List<String> phantom) {
        StringBuilder sb = new StringBuilder(INDENT + "pub fn " + name + "() -> Self {\n");
        sb.append(INDENT).append(INDENT).append("Self {\n");
        if (kindInit != null) sb.append(INDENT.repeat(3)).append("kind: ").append(kindInit).append(",\n");
        if (base != null) sb.append(INDENT.repeat(3)).append("base: ").append(baseValue(null, null)).append(",\n");
        for (FieldInfo f : fields) {
            sb.append(INDENT.repeat(3)).append(RustNames.snake(f.name())).append(": ").append(initial(f)).append(",\n");
        }
        phantom.forEach(tp -> sb.append(INDENT.repeat(3)).append("_").append(tp.toLowerCase()).append(": PhantomData,\n"));
        return sb.append(INDENT).append(INDENT).append("}\n").append(INDENT).append("}\n").toString();
    }

    /**
     * The positional constructor of a record, with the parameters of the canonical constructor as the facts see them
     * (the analysis may call a parameter independent: borrowed, and copied into the field, a string with to_string, a
     * record or immutable value with clone).
     */
    private String recordConstructor(MethodInfo canonical, List<FieldInfo> fields, List<String> phantom) {
        String params = canonical == null
                ? fields.stream().map(f -> RustNames.snake(f.name()) + ": " + fieldType(f)).collect(Collectors.joining(", "))
                : parameters(canonical);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            String name = RustNames.snake(fields.get(i).name());
            boolean borrowedString = canonical != null && RustTypes.isString(fields.get(i).type())
                                     && context.passing(canonical.parameters().get(i)) != Passing.OWNED;
            if (borrowedString) {
                values.add(name + ": " + name + ".to_string()");
                continue;
            }
            // a record value the facts let the constructor borrow: the field keeps its own copy
            String field = fieldType(fields.get(i));
            String param = canonical == null ? field
                    : parameter(canonical, canonical.parameters().get(i)).replaceFirst("^[^:]*: ", "");
            if (param.equals("&" + field)) values.add(name + ": " + name + ".clone()");
            else if (field.startsWith("Option<") && param.equals("Option<&" + field.substring("Option<".length())))
                values.add(name + ": " + name + ".cloned()");
            else values.add(name);
        }
        String names = String.join(", ", values);
        String phantoms = phantom.stream().map(tp -> ", _" + tp.toLowerCase() + ": PhantomData")
                .collect(Collectors.joining());
        return INDENT + "pub fn new(" + params + ") -> Self {\n" + INDENT + INDENT + "Self { " + names + phantoms
               + " }\n" + INDENT + "}\n";
    }

    private String accessor(FieldInfo f) {
        String name = RustNames.snake(f.name());
        boolean copy = RustTypes.isCopy(f.type());
        String held = fieldType(f);
        if (!copy && held.startsWith("Option<") && held.endsWith(">") && !context.cells.isCell(f)
            && !context.boxes.isBoxed(f)) {
            // a nullable component: an Option<&T>, as a call of any nullable getter reads it (was &Option<T>, and
            // 'initializer.instance().cloned()' E0599)
            String inner = held.substring("Option<".length(), held.length() - 1);
            boolean string = RustTypes.isString(f.type());
            return INDENT + "pub fn " + name + "(&self) -> Option<&" + (string ? "str" : inner) + "> {\n" + INDENT
                   + INDENT + "self." + name + (string ? ".as_deref()" : ".as_ref()") + "\n" + INDENT + "}\n";
        }
        String type = copy ? held : "&" + held;
        return INDENT + "pub fn " + name + "(&self) -> " + type + " {\n" + INDENT + INDENT + (copy ? "" : "&")
               + "self." + name + "\n" + INDENT + "}\n";
    }

    /** A field's value as the struct holds it: in a {@code RefCell} when it is one (see {@link RustCells}). */
    private String held(FieldInfo f, String value) {
        // a nullable field's value is None, Some(...) or an Option already
        if (context.boxes.isBoxed(f)) value = RustBoxes.box(value, RustFacts.nullable(context.facts.field(f)), true);
        return context.cells.isCell(f) ? "RefCell::new(" + value + ")" : value;
    }

    /** The value of a field the constructor does not assign: its initializer, or the empty value of its type. */
    private String initial(FieldInfo f) {
        return held(f, plainInitial(f));
    }

    private String plainInitial(FieldInfo f) {
        Expression init = f.initializer();
        boolean nullable = RustFacts.nullable(context.facts.field(f));
        if (init != null && !(init instanceof EmptyExpression)) {
            String value = new RustBodyPrinter(context).ownedText(init);
            return nullable && !"None".equals(value) ? "Some(" + value + ")" : value;
        }
        return RustTypes.empty(f.type(), nullable, context, f);
    }

    private String constructor(MethodInfo c, List<FieldInfo> fields, List<String> phantom) {
        context.method = c;
        try {
            StringBuilder sb = new StringBuilder(INDENT + visibility(c.access().isPrivate()) + "fn " + RustNames.method(c)
                                                 + "(" + parameters(c) + ") -> Self {\n");
            Block body = c.methodBody();
            List<Statement> statements = body == null ? List.of() : body.statements();
            String pad = INDENT.repeat(2);
            if (statements.size() == 1 && statements.getFirst() instanceof ExplicitConstructorInvocation eci
                && !eci.isSuper()) {
                // this(...): the other constructor builds it
                RustBodyPrinter printer = new RustBodyPrinter(context);
                return sb.append(pad).append("Self::").append(RustNames.method(eci.methodInfo())).append("(")
                        .append(printer.arguments(eci.parameterExpressions(), eci.methodInfo())).append(")\n")
                        .append(INDENT).append("}\n").toString();
            }
            Map<FieldInfo, Expression> assigned = simpleAssignments(statements, fields);
            RustBodyPrinter printer = new RustBodyPrinter(context);
            if (assigned != null) {
                StringBuilder lets = new StringBuilder();
                for (Statement s : statements) {
                    if (s instanceof ExplicitConstructorInvocation) continue;
                    if (assignedField(s, fields) == null) lets.append(printer.statement(s, 2));
                }
                sb.append(lets).append(pad).append("Self {\n");
                if (kindInit != null) sb.append(pad).append(INDENT).append("kind: ").append(kindInit).append(",\n");
                if (base != null) {
                    sb.append(pad).append(INDENT).append("base: ").append(baseValue(statements, printer)).append(",\n");
                }
                for (FieldInfo f : fields) {
                    Expression value = assigned.get(f);
                    String text = value == null ? initial(f)
                            : context.borrows.fields(f.owner()).containsKey(f) ? printer.text(value)
                            : fieldValue(printer, f, value);
                    sb.append(pad).append(INDENT).append(RustNames.snake(f.name())).append(": ").append(text).append(",\n");
                }
                phantom.forEach(tp -> sb.append(pad).append(INDENT).append("_").append(tp.toLowerCase())
                        .append(": PhantomData,\n"));
                return sb.append(pad).append("}\n").append(INDENT).append("}\n").toString();
            }
            // the general shape: build 'this' at the empty values, run the body on it
            sb.append(pad).append("let mut this = Self {\n");
            if (kindInit != null) sb.append(pad).append(INDENT).append("kind: ").append(kindInit).append(",\n");
            if (base != null) {
                sb.append(pad).append(INDENT).append("base: ").append(baseValue(statements, printer)).append(",\n");
            }
            for (FieldInfo f : fields) {
                sb.append(pad).append(INDENT).append(RustNames.snake(f.name())).append(": ").append(initial(f)).append(",\n");
            }
            phantom.forEach(tp -> sb.append(pad).append(INDENT).append("_").append(tp.toLowerCase()).append(": PhantomData,\n"));
            sb.append(pad).append("};\n");
            String outer = context.receiver;
            context.receiver = "this";
            try {
                for (Statement s : statements) {
                    if (s instanceof ExplicitConstructorInvocation) continue;
                    sb.append(printer.statement(s, 2));
                }
            } finally {
                context.receiver = outer;
            }
            return sb.append(pad).append("this\n").append(INDENT).append("}\n").toString();
        } finally {
            context.method = null;
        }
    }

    private String fieldValue(RustBodyPrinter printer, FieldInfo f, Expression value) {
        return held(f, plainFieldValue(printer, f, value));
    }

    private String plainFieldValue(RustBodyPrinter printer, FieldInfo f, Expression value) {
        return printer.fieldValue(value, f.type(), RustFacts.nullable(context.facts.field(f)));
    }

    /**
     * The fields the body assigns, each once, as a top-level statement whose value reads no field of the object;
     * {@code null} when the body does anything else to the object, so that the general shape is needed.
     */
    private static Map<FieldInfo, Expression> simpleAssignments(List<Statement> statements, List<FieldInfo> fields) {
        Map<FieldInfo, Expression> assigned = new LinkedHashMap<>();
        for (Statement s : statements) {
            if (s instanceof ExplicitConstructorInvocation eci) {
                if (!eci.isSuper()) return null;
                continue;
            }
            FieldInfo f = assignedField(s, fields);
            if (f != null) {
                Expression value = ((Assignment) ((ExpressionAsStatement) s).expression()).value();
                if (assigned.containsKey(f) || readsThis(value)) return null;
                assigned.put(f, value);
            } else if (readsThis(s)) {
                return null;
            }
        }
        return assigned;
    }

    private static FieldInfo assignedField(Statement s, List<FieldInfo> fields) {
        if (s instanceof ExpressionAsStatement es && es.expression() instanceof Assignment a
            && (a.assignmentOperator() == null || "=".equals(a.assignmentOperator().name()))
            && a.target().variable() instanceof FieldReference fr && fr.scopeIsThis()
            && fields.contains(fr.fieldInfo())) {
            return fr.fieldInfo();
        }
        return null;
    }

    private static boolean readsThis(Element element) {
        boolean[] found = {false};
        element.visit(e -> {
            if (e instanceof VariableExpression ve && (ve.variable() instanceof This
                || ve.variable() instanceof FieldReference fr && !fr.fieldInfo().isStatic() && fr.scopeIsThis())) {
                found[0] = true;
            }
            if (e instanceof MethodCall mc && !mc.methodInfo().isStatic()
                && mc.objectIsImplicit()) {
                found[0] = true;
            }
            if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    // ---- methods

    private String method(MethodInfo m) {
        return method(m, false);
    }

    private String method(MethodInfo m, boolean inTrait) {
        if (m.isSynchronized()) context.message(RustPrintCode.SYNCHRONIZED, m, m.name());
        StringBuilder sb = new StringBuilder(INDENT).append(signature(m, !inTrait));
        Block body = m.methodBody();
        if (m.isAbstract() || body == null) {
            return sb.append(" {\n").append(INDENT).append(INDENT).append("unimplemented!()\n").append(INDENT)
                    .append("}\n").toString();
        }
        if (unsafeMethod(m, context)) {
            return sb.append(" {\n").append(INDENT).append(INDENT).append("unsafe {\n").append(body(m, 3)).append(INDENT)
                    .append(INDENT).append("}\n").append(INDENT).append("}\n").toString();
        }
        sb.append(" {\n").append(body(m, 2)).append(INDENT).append("}\n");
        if (context.mutableGetters.contains(m)) sb.append(mutableGetter(m, inTrait));
        return sb.toString();
    }

    /** The exclusive twin of a getter a caller modifies through ({@link RustGetters}): the field, borrowed mutably. */
    private String mutableGetter(MethodInfo m, boolean inTrait) {
        FieldInfo f = RustGetters.field(m);
        if (f == null || context.boxes.isBoxed(f) || context.cells.isCell(f)) return "";
        RustTypes.Form borrowed = context.borrows.fields(f.owner()).get(f);
        // a field that borrows exclusively (ExceptionRangeCFG's '&'a mut Vec<…>' protected range) hands out a reborrow
        boolean exclusiveBorrow = borrowed != null && fieldType(f).replaceFirst("^Option<", "")
                .startsWith("&" + RustBorrows.LIFETIME + " mut ");
        if (borrowed != null && !exclusiveBorrow) return "";
        // in an impl of a trait, the twin the trait declares (RustGetters): as the trait declares it, no visibility
        MethodInfo declared = inTrait ? m.overrides().stream().filter(RustGetters::abstractGetter)
                .filter(context.mutableGetters::contains).findFirst().orElse(null) : null;
        if (inTrait && declared == null) return "";
        boolean nullable = RustFacts.nullable(context.facts.field(f));
        // a field a class below a hierarchy's root adds is in its data, in the kind (RustHierarchies)
        String field = RustHierarchies.current().isSubMember(f.owner())
                ? "self.kind." + RustHierarchies.accessor(f.owner()) + "_mut()." + RustNames.snake(f.name())
                : "self." + RustNames.snake(f.name());
        String signature = declared != null ? mutableGetterSignature(declared)
                : visibility(m.access().isPrivate()) + mutableGetterSignature(m, f.type(), nullable);
        String body = exclusiveBorrow ? (nullable ? field + ".as_deref_mut()" : "&mut *" + field)
                : nullable ? field + ".as_mut()" : "&mut " + field;
        return INDENT + signature + " {\n" + INDENT + INDENT + body + "\n" + INDENT + "}\n";
    }

    /* an interface getter's twin, from its declared result */
    private String mutableGetterSignature(MethodInfo m) {
        return mutableGetterSignature(m, m.returnType(), RustFacts.nullable(context.facts.returnType(m)));
    }

    private String mutableGetterSignature(MethodInfo m, ParameterizedType type, boolean nullable) {
        String rust = RustTypes.owned(type, context, m);
        return "fn " + RustNames.method(m) + RustGetters.SUFFIX + "(&mut self) -> "
               + (nullable ? "Option<&mut " + rust + ">" : "&mut " + rust);
    }

    /**
     * The {@code @Mark} of a typestate: it consumes the object in its before state, runs the body on it, and moves its
     * fields into the object in its after state.
     */
    private String mark(MethodInfo m, String name, List<FieldInfo> fields, List<String> phantom) {
        StringBuilder sb = new StringBuilder(INDENT).append(signature(m, true)).append(" {\n");
        Block body = m.methodBody();
        if (body != null) sb.append(body(m, 2));
        String pad = INDENT.repeat(2);
        sb.append(pad).append(name).append(" {\n");
        for (FieldInfo f : fields) {
            String field = RustNames.snake(f.name());
            sb.append(pad).append(INDENT).append(field).append(": self.").append(field).append(",\n");
        }
        phantom.forEach(tp -> sb.append(pad).append(INDENT).append("_").append(tp.toLowerCase()).append(": PhantomData,\n"));
        return sb.append(pad).append("}\n").append(INDENT).append("}\n").toString();
    }

    private String body(MethodInfo m, int depth) {
        context.method = m;
        try {
            return new RustBodyPrinter(context).statements(m.methodBody(), depth);
        } finally {
            context.method = null;
        }
    }

    // the lifetime a static function gives the structs with a lifetime in its signature, outside such a struct's impl
    private static final String STRUCT_LIFETIME = "'s";

    /** {@code pub fn name<T>(&self, p: &P) -> R}: the receiver and the parameters as the facts decide. */
    private String signature(MethodInfo m, boolean pub) {
        Receiver receiver = context.facts.receiver(m);
        if (markOfTypestate(m)) {
            // the transition: the object in its before state is consumed, and returned in its after state
            return (pub ? visibility(m.access().isPrivate()) : "") + "fn " + name(m) + "(mut self"
                   + (m.parameters().isEmpty() ? "" : ", " + parameters(m)) + ") -> " + RustNames.type(m.typeInfo())
                   + "<" + RustTypestate.AFTER + ">";
        }
        String self = switch (receiver) {
            case NONE -> null;
            case SHARED -> "&self";
            case EXCLUSIVE -> "&mut self";
            case OWNED -> "self";
        };
        String params = parameters(m);
        String all = self == null ? params : params.isEmpty() ? self : self + ", " + params;
        String result = resultType(m);
        String generics = bounded(m.typeParameters(), false, context.facts.resultBorrows(m) != null
                                                             || RESULT_LIFETIME.equals(keptLifetime(m)));
        if (self == null && (result.contains("'_") || all.split("'_", -1).length > 2)) {
            // a static function outside a struct with the lifetime, returning a struct that has one
            // (ClassWriter.findAssertionField(node: &'r ClassNode<'_>) -> Option<&'r Shared<StructMember<'_>>>): with
            // more than one input lifetime the result's cannot be elided (E0106); the structs' lifetime is the function's.
            // Or taking two of them: Shared<T> and &mut T are invariant, so 'wrapper: &mut ClassWrapper<'_>, method:
            // &Shared<MethodWrapper<'_>>' cannot share a Statement<'_>; Java has one lifetime, the program's
            all = all.replace("'_", STRUCT_LIFETIME);
            result = result.replace("'_", STRUCT_LIFETIME);
            generics = generics.isEmpty() ? "<" + STRUCT_LIFETIME + ">" : "<" + STRUCT_LIFETIME + ", " + generics.substring(1);
        }
        return (pub ? visibility(m.access().isPrivate()) : "") + (unsafeMethod(m, context) ? "unsafe " : "") + "fn "
               + name(m) + generics + "(" + all + ")" + result;
    }

    private String name(MethodInfo m) {
        return methodName != null ? methodName : RustNames.method(m);
    }

    private boolean markOfTypestate(MethodInfo m) {
        return context.facts.side(m) == RustFacts.Side.MARK && RustTypestate.typestate(m.typeInfo(), context);
    }

    /** {@code " -> T"}, or empty for a void method. */
    private String resultType(MethodInfo m) {
        if (m.isVoid()) return "";
        Receiver receiver = context.facts.receiver(m);
        if (context.facts.fluent(m) && (receiver == Receiver.SHARED || receiver == Receiver.EXCLUSIVE)) {
            return receiver == Receiver.SHARED ? " -> &Self" : " -> &mut Self";
        }
        boolean borrows = (context.facts.resultBorrowsReceiver(m) && receiver != Receiver.NONE
                           || context.facts.resultBorrows(m) != null)
                          && !RustTypes.isCopy(m.returnType());
        String type = RustTypes.type(m.returnType(), borrows ? Form.SHARED : Form.OWNED, context, m);
        // a part of one parameter's object: named, as &self would otherwise take the elided lifetime
        if (context.facts.resultBorrows(m) != null) type = type.replaceFirst("&", "&" + RESULT_LIFETIME + " ");
        // a new struct borrowing parameters, outside its own impl: their lifetime, not self's
        if (RESULT_LIFETIME.equals(keptLifetime(m))) type = type.replaceFirst("<'_", "<" + RESULT_LIFETIME);
        if (receiver != Receiver.NONE && RustTypes.streamItem(m.returnType(), context, m) != null) {
            // a stream over the object's contents borrows it
            type = type.replaceFirst(">>$", "> + '_>");
        }
        return " -> " + RustTypes.option(type, RustFacts.nullable(context.facts.returnType(m)));
    }

    private String parameters(MethodInfo m) {
        List<String> out = new ArrayList<>();
        for (ParameterInfo p : m.parameters()) out.add(parameter(m, p));
        return String.join(", ", out);
    }

    private String parameter(MethodInfo m, ParameterInfo p) {
        ParameterizedType type = p.parameterizedType();
        boolean nullable = RustFacts.nullable(context.facts.parameter(p));
        String rust;
        boolean mut = m.methodBody() != null && reassigned(m.methodBody(), p);
        if (RustTypes.isCopy(type)) {
            rust = RustTypes.owned(type, context, p);
        } else {
            Passing passing = context.passing(p);
            Form form = switch (passing) {
                case SHARED -> Form.SHARED;
                case EXCLUSIVE -> Form.EXCLUSIVE;
                case OWNED -> Form.OWNED;
            };
            rust = RustTypes.type(type, form, context, p);
            if (context.mutatingFunctions.contains(p)) {
                // a call site passes a closure that writes what it captures (RustClosures)
                rust = rust.replaceFirst("dyn Fn\\(", "dyn FnMut(");
                mut |= passing == Passing.OWNED;
            }
            // stored in a borrowed field: the borrow lives as long as the object
            if (context.borrows.parameter(p) != null) rust = rust.replaceFirst("^&", "&" + RustBorrows.LIFETIME + " ");
            // the result is part of it: it lives as long as the result
            else if (p.equals(context.facts.resultBorrows(m))) rust = rust.replaceFirst("^&", "&" + RESULT_LIFETIME + " ");
            // kept, borrowed, in the new struct the method returns: the struct's lifetime
            else if (keptLifetime(m) != null && context.facts.keptInNewResult(p) && passing != Passing.OWNED) {
                rust = rust.replaceFirst("^&", "&" + keptLifetime(m) + " ");
            }
            // an owned value the method modifies needs a mutable binding; so does an Option<&mut T>, reached by as_mut()
            mut |= passing == Passing.OWNED && !p.isUnmodified() || passing == Passing.EXCLUSIVE && nullable;
        }
        return (mut ? "mut " : "") + RustNames.snake(p.name()) + ": " + RustTypes.option(rust, nullable);
    }

    static boolean reassigned(Block body, ParameterInfo p) {
        boolean[] found = {false};
        body.visit(e -> {
            if (e instanceof Assignment a && p.equals(a.variableTarget())) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    // ---- small things

    private static String generics(List<TypeParameter> typeParameters) {
        return generics(typeParameters, false);
    }

    /** With {@code lifetime}, the struct's {@code 'a} first: it has borrowed fields (see {@link RustBorrows}). */
    private static String generics(List<TypeParameter> typeParameters, boolean lifetime) {
        if (typeParameters.isEmpty() && !lifetime) return "";
        List<String> names = new ArrayList<>();
        if (lifetime) names.add(RustBorrows.LIFETIME);
        typeParameters.forEach(tp -> names.add(RustNames.typeParameter(tp)));
        return "<" + String.join(", ", names) + ">";
    }

    /** {@link #generics}, each type parameter with the bounds its uses need ({@link RustBounds}). */
    private String bounded(List<TypeParameter> typeParameters, boolean lifetime) {
        return bounded(typeParameters, lifetime, false);
    }

    /** ... and with {@code resultLifetime}, the {@link #RESULT_LIFETIME} a result borrowing a parameter shares with it. */
    private String bounded(List<TypeParameter> typeParameters, boolean lifetime, boolean resultLifetime) {
        if (typeParameters.isEmpty() && !lifetime && !resultLifetime) return "";
        List<String> names = new ArrayList<>();
        if (lifetime) names.add(RustBorrows.LIFETIME);
        if (resultLifetime) names.add(RESULT_LIFETIME);
        typeParameters.forEach(tp -> names.add(context.bounds.declare(tp)));
        return "<" + String.join(", ", names) + ">";
    }

    /**
     * The lifetime of the new struct {@code m} returns, which borrows what it keeps ({@link RustBorrows}): the impl's
     * {@code 'a} in the struct's own impl, {@link #RESULT_LIFETIME} elsewhere; {@code null} when the result keeps no
     * borrowed parameter.
     */
    private String keptLifetime(MethodInfo m) {
        TypeInfo result = m.returnType().typeInfo();
        if (result == null || m.returnType().arrays() > 0 || !context.borrows.hasLifetime(result)) return null;
        boolean keeps = m.parameters().stream().anyMatch(p -> context.facts.keptInNewResult(p)
                                                               && !RustTypes.isCopy(p.parameterizedType())
                                                               && context.passing(p) != Passing.OWNED);
        if (!keeps) return null;
        return result == context.type ? RustBorrows.LIFETIME : RESULT_LIFETIME;
    }

    /** The lifetime of a result that borrows one of its method's parameters (RustFacts.resultBorrows). */
    static final String RESULT_LIFETIME = "'r";

    private static String visibility(boolean isPrivate) {
        return isPrivate ? "" : "pub ";
    }

    private static String doc(TypeInfo typeInfo) {
        return "/// Translated from `" + typeInfo.fullyQualifiedName() + "`.\n";
    }
}
