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

import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.EmptyExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
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
import io.codelaser.maddi.cst.print.rust.RustFacts.Passing;
import io.codelaser.maddi.cst.print.rust.RustFacts.Receiver;
import io.codelaser.maddi.cst.print.rust.RustTypes.Form;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            if (typeInfo.isInterface()) sb.append(trait(typeInfo));
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
    static boolean plainEnum(TypeInfo typeInfo) {
        return typeInfo.fields().stream().noneMatch(f -> !f.isStatic() && !f.isSynthetic())
               && typeInfo.constructors().stream().allMatch(c -> c.parameters().isEmpty());
    }

    // ---- items

    private String trait(TypeInfo typeInfo) {
        StringBuilder sb = new StringBuilder(doc(typeInfo));
        sb.append(visibility(typeInfo.isPrivate())).append("trait ").append(RustNames.type(typeInfo))
                .append(generics(typeInfo.typeParameters())).append(supertraits(typeInfo)).append(" {\n");
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
            sb.append(INDENT).append(bodyless ? signature.replaceAll("\\bmut ((r#)?\\w+): ", "$1: ") : signature);
            if (bodyless) sb.append(";\n");
            else sb.append(" {\n").append(body(m, 2)).append(INDENT).append("}\n");
        }
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
        if (!plainEnum(typeInfo)) context.message(RustPrintCode.ENUM_WITH_STATE, typeInfo, typeInfo.simpleName());
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
        this.base = base == null ? null : RustTypes.owned(parent, context, typeInfo);
        List<FieldInfo> fields = typeInfo.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic()).toList();
        String name = RustNames.type(typeInfo);
        boolean typestate = RustTypestate.typestate(typeInfo, context);
        String generics = typestate ? "<" + RustTypestate.STATE + ">"
                : generics(typeInfo.typeParameters(), context.borrows.hasLifetime(typeInfo));
        // the trait bounds its members need (RustBounds); @Immutable(hc=true) shares its hidden content by cloning
        String implGenerics = typestate ? generics
                : bounded(typeInfo.typeParameters(), context.borrows.hasLifetime(typeInfo));
        List<String> phantom = new ArrayList<>(phantomTypeParameters(typeInfo, fields));
        if (typestate) {
            // the state of the transition, a type that no field holds
            phantom.add(RustTypestate.STATE);
            context.use("crate::jrt::" + RustTypestate.BEFORE);
            context.use("crate::jrt::" + RustTypestate.AFTER);
            context.message(RustPrintCode.TYPESTATE, typeInfo, typeInfo.simpleName());
        }

        StringBuilder sb = new StringBuilder(doc(typeInfo));
        if (record) sb.append("#[derive(Debug, Clone, PartialEq)]\n");
        else if (context.facts.immutable(typeInfo)) sb.append("#[derive(Clone)]\n");
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
            sb.append("}\n");
        }
        if (equals != null) sb.append(partialEq(equals, name, generics, implGenerics));
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
            sb.append("\nimpl").append(implGenerics).append(" std::hash::Hash for ").append(name).append(generics)
                    .append(" {\n").append(INDENT).append("fn hash<H: std::hash::Hasher>(&self, state: &mut H) {\n")
                    .append(INDENT).append(INDENT).append("state.write_i32(self.hash_code());\n").append(INDENT)
                    .append("}\n}\n");
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
        if (statements != null) {
            for (Statement s : statements) {
                if (s instanceof ExplicitConstructorInvocation eci && eci.isSuper() && eci.methodInfo() != null) {
                    return owner + "::" + RustNames.method(eci.methodInfo()) + "("
                           + printer.arguments(eci.parameterExpressions(), eci.methodInfo()) + ")";
                }
            }
        }
        return owner + "::new()";
    }

    /** The superclass, when it is a source class: its state becomes a {@code base} field. */
    static TypeInfo sourceParent(TypeInfo typeInfo) {
        ParameterizedType parent = typeInfo.parentClass();
        if (parent == null || parent.typeInfo() == null) return null;
        TypeInfo p = parent.typeInfo();
        if (p.isJavaLangObject() || p.compilationUnit().externalLibrary() || p.isInterface()) return null;
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
        Set<TypeInfo> traits = new java.util.HashSet<>();
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
    static boolean lazyStatic(FieldInfo f, RustContext context) {
        if (!f.isStatic() || !f.isFinal() || f.initializer() == null || f.initializer().isEmpty()
            || constant(f) || !context.facts.unmodified(f)) {
            return false;
        }
        String type = RustTypes.owned(f.type(), context, f);
        if (type.contains("dyn ") || type.contains("Rc<") || type.contains("RefCell<")) return false;
        boolean[] touched = {false};
        for (MethodInfo m : f.owner().methods()) {
            if (!m.isStaticInitializer() || m.methodBody() == null) continue;
            m.methodBody().visit(e -> {
                if (e instanceof io.codelaser.maddi.cst.api.expression.VariableExpression ve
                    && ve.variable() instanceof io.codelaser.maddi.cst.api.variable.FieldReference fr
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
     * {@code VecDeque}, {@code BTreeMap}, {@code BTreeSet}), initialized with a constant or not at all. {@code null}
     * for any other static field.
     */
    static String mutableStaticValue(FieldInfo f, RustContext context) {
        if (!f.isStatic() || f.isFinal() || f.owner().compilationUnit().externalLibrary()) return null;
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
        return java.util.Set.of("String", "Vec", "VecDeque", "BTreeMap", "BTreeSet").contains(base) ? base + "::new()"
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

    /** The module-level name of a {@link #lazyStatic} field: its owner's name and its own. */
    static String staticName(FieldInfo f) {
        return RustNames.constant(RustNames.type(f.owner()) + "_" + f.name());
    }

    /** A {@code static final} primitive or string with a literal value: an associated {@code const}. */
    static boolean constant(FieldInfo f) {
        return f.isFinal() && f.initializer() instanceof ConstantExpression<?>
               && (f.type().isPrimitiveExcludingVoid() || RustTypes.isString(f.type()));
    }

    private String staticField(FieldInfo f) {
        Expression init = f.initializer();
        boolean constant = constant(f);
        if (!constant && lazyStatic(f, context)) {
            String type = RustTypes.owned(f.type(), context, f);
            String value = new RustBodyPrinter(context).ownedAs(init, f.type());
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
        return INDENT + visibility(f.access().isPrivate()) + "const " + RustNames.constant(f.name()) + ": " + type + " = "
               + value + ";\n";
    }

    // ---- constructors

    private String defaultConstructor(List<FieldInfo> fields, List<String> phantom) {
        StringBuilder sb = new StringBuilder(INDENT + "pub fn new() -> Self {\n");
        sb.append(INDENT).append(INDENT).append("Self {\n");
        if (base != null) sb.append(INDENT.repeat(3)).append("base: ").append(baseValue(null, null)).append(",\n");
        for (FieldInfo f : fields) {
            sb.append(INDENT.repeat(3)).append(RustNames.snake(f.name())).append(": ").append(initial(f)).append(",\n");
        }
        phantom.forEach(tp -> sb.append(INDENT.repeat(3)).append("_").append(tp.toLowerCase()).append(": PhantomData,\n"));
        return sb.append(INDENT).append(INDENT).append("}\n").append(INDENT).append("}\n").toString();
    }

    /**
     * The positional constructor of a record, with the parameters of the canonical constructor as the facts see them
     * (the analysis may call a string parameter independent: borrowed, and copied into the field).
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
            values.add(borrowedString ? name + ": " + name + ".to_string()" : name);
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
        String type = copy ? fieldType(f) : "&" + fieldType(f);
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

    private static boolean readsThis(io.codelaser.maddi.cst.api.element.Element element) {
        boolean[] found = {false};
        element.visit(e -> {
            if (e instanceof VariableExpression ve && (ve.variable() instanceof io.codelaser.maddi.cst.api.variable.This
                || ve.variable() instanceof FieldReference fr && !fr.fieldInfo().isStatic() && fr.scopeIsThis())) {
                found[0] = true;
            }
            if (e instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc && !mc.methodInfo().isStatic()
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
        if (!inTrait && context.mutableGetters.contains(m)) sb.append(mutableGetter(m));
        return sb.toString();
    }

    /** The exclusive twin of a getter a caller modifies through ({@link RustGetters}): the field, borrowed mutably. */
    private String mutableGetter(MethodInfo m) {
        FieldInfo f = RustGetters.field(m);
        if (f == null || context.boxes.isBoxed(f) || context.cells.isCell(f)
            || context.borrows.fields(f.owner()).containsKey(f)) {
            return "";
        }
        boolean nullable = RustFacts.nullable(context.facts.field(f));
        String type = RustTypes.owned(f.type(), context, m);
        String field = "self." + RustNames.snake(f.name());
        return INDENT + visibility(m.access().isPrivate()) + "fn " + RustNames.method(m) + RustGetters.SUFFIX
               + "(&mut self) -> " + (nullable ? "Option<&mut " + type + ">" : "&mut " + type) + " {\n"
               + INDENT + INDENT + (nullable ? field + ".as_mut()" : "&mut " + field) + "\n" + INDENT + "}\n";
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

    /** {@code pub fn name<T>(&self, p: &P) -> R}: the receiver and the parameters as the facts decide. */
    private String signature(MethodInfo m, boolean pub) {
        Receiver receiver = context.facts.receiver(m);
        if (markOfTypestate(m)) {
            // the transition: the object in its before state is consumed, and returned in its after state
            return (pub ? visibility(m.access().isPrivate()) : "") + "fn " + RustNames.method(m) + "(mut self"
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
        return (pub ? visibility(m.access().isPrivate()) : "") + (unsafeMethod(m, context) ? "unsafe " : "") + "fn "
               + RustNames.method(m) + bounded(m.typeParameters(), false, context.facts.resultBorrows(m) != null
                                                                          || RESULT_LIFETIME.equals(keptLifetime(m)))
               + "(" + all + ")" + result;
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
        typeParameters.forEach(tp -> names.add(tp.simpleName()));
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
