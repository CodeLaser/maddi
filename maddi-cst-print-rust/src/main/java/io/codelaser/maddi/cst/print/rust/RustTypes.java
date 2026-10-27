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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Java types as Rust types, in three forms: owned ({@code String}, {@code Vec<T>}, {@code Box<dyn I>}), shared
 * ({@code &str}, {@code &Vec<T>}, {@code &dyn I}) and exclusive ({@code &mut String}, …). A primitive is the same in
 * all three: it is copied. The JDK's collections map onto {@code std}'s; a JDK type outside the mapping is reported.
 */
final class RustTypes {

    /** The form of a type at a declaration: see {@link RustFacts.Passing}. */
    enum Form {OWNED, SHARED, EXCLUSIVE}

    private record Mapped(String rust, String use) {
    }

    /** The JDK's types with a {@code std} counterpart, by fully qualified name. */
    private static final Map<String, Mapped> JDK = Map.ofEntries(
            Map.entry("java.lang.String", new Mapped("String", null)),
            Map.entry("java.lang.CharSequence", new Mapped("String", null)),
            Map.entry("java.lang.StringBuilder", new Mapped("String", null)),
            Map.entry("java.lang.Integer", new Mapped("i32", null)),
            Map.entry("java.lang.Long", new Mapped("i64", null)),
            Map.entry("java.lang.Short", new Mapped("i16", null)),
            Map.entry("java.lang.Byte", new Mapped("i8", null)),
            Map.entry("java.lang.Double", new Mapped("f64", null)),
            Map.entry("java.lang.Float", new Mapped("f32", null)),
            Map.entry("java.lang.Boolean", new Mapped("bool", null)),
            Map.entry("java.lang.Character", new Mapped("char", null)),
            Map.entry("java.math.BigDecimal", new Mapped("f64", null)),
            Map.entry("java.time.LocalDateTime", new Mapped("SystemTime", "std::time::SystemTime")),
            Map.entry("java.time.Instant", new Mapped("SystemTime", "std::time::SystemTime")),
            Map.entry("java.util.Collection", new Mapped("Vec", null)),
            Map.entry("java.lang.Iterable", new Mapped("Vec", null)),
            Map.entry("java.util.List", new Mapped("Vec", null)),
            Map.entry("java.util.ArrayList", new Mapped("Vec", null)),
            Map.entry("java.util.LinkedList", new Mapped("Vec", null)),
            Map.entry("java.util.Set", new Mapped("HashSet", "std::collections::HashSet")),
            Map.entry("java.util.HashSet", new Mapped("HashSet", "std::collections::HashSet")),
            Map.entry("java.util.LinkedHashSet", new Mapped("HashSet", "std::collections::HashSet")),
            Map.entry("java.util.SortedSet", new Mapped("BTreeSet", "std::collections::BTreeSet")),
            Map.entry("java.util.NavigableSet", new Mapped("BTreeSet", "std::collections::BTreeSet")),
            Map.entry("java.util.TreeSet", new Mapped("BTreeSet", "std::collections::BTreeSet")),
            Map.entry("java.util.Map", new Mapped("HashMap", "std::collections::HashMap")),
            Map.entry("java.util.HashMap", new Mapped("HashMap", "std::collections::HashMap")),
            Map.entry("java.util.LinkedHashMap", new Mapped("HashMap", "std::collections::HashMap")),
            Map.entry("java.util.SortedMap", new Mapped("BTreeMap", "std::collections::BTreeMap")),
            Map.entry("java.util.NavigableMap", new Mapped("BTreeMap", "std::collections::BTreeMap")),
            Map.entry("java.util.TreeMap", new Mapped("BTreeMap", "std::collections::BTreeMap")),
            Map.entry("java.util.Queue", new Mapped("VecDeque", "std::collections::VecDeque")),
            Map.entry("java.util.Deque", new Mapped("VecDeque", "std::collections::VecDeque")),
            Map.entry("java.util.ArrayDeque", new Mapped("VecDeque", "std::collections::VecDeque")),
            Map.entry("java.util.Optional", new Mapped("Option", null)),
            // maddi's own eventually final holder: set once (@Mark), read after (@Only(after))
            Map.entry("io.codelaser.maddi.support.SetOnce", new Mapped("Option", null)),
            Map.entry("java.util.OptionalInt", new Mapped("Option", null)),
            Map.entry("java.util.OptionalLong", new Mapped("Option", null)),
            Map.entry("java.util.OptionalDouble", new Mapped("Option", null)));

    private RustTypes() {
    }

    static boolean isString(ParameterizedType type) {
        return type.typeInfo() != null && type.arrays() == 0 && "String".equals(mapped(type.typeInfo()));
    }

    /**
     * A source class or record the facts call {@code @Immutable} (no hidden content): printed with
     * {@code #[derive(Clone)]}, and cloned where an owned value is expected, as a string is.
     */
    static boolean isImmutableValue(ParameterizedType type, RustFacts facts) {
        if (type.arrays() > 0) return false;
        if (type.isTypeParameter()) {
            // the hidden content of an @Immutable(hc=true) type, which its impl bounds by Clone
            var owner = type.typeParameter().getOwner();
            return owner.isLeft() && facts.immutableHc(owner.getLeft());
        }
        TypeInfo ti = type.typeInfo();
        return ti != null && !ti.compilationUnit().externalLibrary() && !ti.isInterface()
               && !ti.typeNature().isEnum() && (facts.immutable(ti) || facts.immutableHc(ti));
    }

    /** {@code @Immutable(hc=true)} source class or record: held as an {@code Rc}, cloned to share it. */
    static boolean isShared(TypeInfo ti, RustFacts facts) {
        return ti != null && !ti.compilationUnit().externalLibrary() && !ti.isInterface() && !ti.typeNature().isEnum()
               && facts.immutableHc(ti);
    }

    /** Copied rather than moved or borrowed: a primitive, a boxed primitive, a plain enum. */
    static boolean isCopy(ParameterizedType type) {
        if (type.arrays() > 0 || type.typeInfo() == null) return false;
        TypeInfo ti = type.typeInfo();
        if (ti.isPrimitiveExcludingVoid() || ti.isBoxedExcludingVoid()) return true;
        if ("java.math.BigDecimal".equals(ti.fullyQualifiedName())) return true;
        if ("SystemTime".equals(mapped(ti))) return true;
        return ti.typeNature().isEnum() && RustTypePrinter.plainEnum(ti);
    }

    /** {@code Vec}, {@code HashMap}, … for a JDK collection type; {@code null} otherwise. */
    static String mapped(TypeInfo typeInfo) {
        Mapped m = JDK.get(typeInfo.fullyQualifiedName());
        return m == null ? null : m.rust;
    }

    /** {@code type} in {@code form}, as a declaration writes it. */
    static String type(ParameterizedType type, Form form, RustContext context, Element where) {
        if (type.isPrimitiveExcludingVoid() && type.arrays() == 0) return primitive(type.typeInfo());
        if (type.isVoid()) return "()";
        TypeInfo ti = type.typeInfo();
        if (form == Form.SHARED && isString(type)) return "&str";
        if (form != Form.OWNED && ti != null && type.arrays() == 0 && streamItem(type, context, where) != null) {
            // a stream parameter is consumed, as Java's is: any iterator of the items
            return "impl Iterator<Item = " + streamItem(type, context, where) + ">";
        }
        if (form != Form.OWNED && ti != null && type.arrays() == 0 && isEntry(ti)) {
            return (form == Form.SHARED ? "&" : "&mut ") + owned(type, context, where);
        }
        if (form != Form.OWNED && type.arrays() > 0) {
            // a Java array never changes length: a borrowed one is a slice, which a &Vec coerces to
            String element = owned(type.copyWithArrays(type.arrays() - 1), context, where);
            return (form == Form.SHARED ? "&[" : "&mut [") + element + "]";
        }
        if (form != Form.OWNED && ti != null && type.arrays() == 0 && isInterface(ti)) {
            // a borrowed closure is passed as '&|| …' or '&mut || …' (RustBodyPrinter.argument), never a borrowed Box
            String trait = isFunctional(ti) ? function(type, context, where) : "dyn " + named(type, context, where);
            return (form == Form.SHARED ? "&" : "&mut ") + trait;
        }
        String owned = owned(type, context, where);
        return switch (form) {
            case OWNED -> owned;
            case SHARED -> "&" + owned;
            case EXCLUSIVE -> "&mut " + owned;
        };
    }

    /** {@code type} wrapped in {@code Option} when nullable. */
    static String option(String rust, boolean nullable) {
        return nullable ? "Option<" + rust + ">" : rust;
    }

    static String owned(ParameterizedType type, RustContext context, Element where) {
        if (type.arrays() > 0) {
            return "Vec<" + owned(type.copyWithArrays(type.arrays() - 1), context, where) + ">";
        }
        if (type.isPrimitiveExcludingVoid()) return primitive(type.typeInfo());
        if (type.isTypeParameter()) return RustNames.typeParameter(type.typeParameter());
        TypeInfo ti = type.typeInfo();
        if (ti == null) {
            // an unbounded wildcard, '?'
            context.use("std::any::Any");
            return "Box<dyn Any>";
        }
        if (isFunctional(ti)) return "Box<" + function(type, context, where) + ">";
        String item = streamItem(type, context, where);
        if (item != null) return "Box<dyn Iterator<Item = " + item + ">>";
        if (isEntry(ti)) {
            // a map's entry is the (key, value) pair its iteration yields; getKey/getValue are .0/.1 (RustJdk.entry)
            List<ParameterizedType> kv = type.parameters();
            if (kv.size() == 2) return "(" + owned(kv.get(0), context, where) + ", " + owned(kv.get(1), context, where) + ")";
            context.use("std::any::Any");
            return "(Box<dyn Any>, Box<dyn Any>)";
        }
        if (isInterface(ti)) return "Box<dyn " + named(type, context, where) + ">";
        return named(type, context, where);
    }

    /**
     * The item of a {@code java.util.stream} stream, which the body prints as an iterator chain: {@code Stream<T>}'s
     * {@code T}, {@code IntStream}'s {@code i32}; {@code null} for another type.
     */
    static String streamItem(ParameterizedType type, RustContext context, Element where) {
        TypeInfo ti = type.typeInfo();
        if (ti == null || type.arrays() > 0) return null;
        return switch (ti.fullyQualifiedName()) {
            case "java.util.stream.Stream" -> type.parameters().size() == 1 ? owned(type.parameters().getFirst(), context, where)
                    : null;
            case "java.util.stream.IntStream" -> "i32";
            case "java.util.stream.LongStream" -> "i64";
            case "java.util.stream.DoubleStream" -> "f64";
            default -> null;
        };
    }

    /** {@code Map.Entry} and its JDK implementations: a tuple. */
    static boolean isEntry(TypeInfo ti) {
        return switch (ti.fullyQualifiedName()) {
            case "java.util.Map.Entry", "java.util.AbstractMap.SimpleEntry", "java.util.AbstractMap.SimpleImmutableEntry"
                    -> true;
            default -> false;
        };
    }

    /** A source interface, not a closure: held as a {@code Box<dyn I>}. */
    static boolean isDyn(ParameterizedType type) {
        TypeInfo ti = type == null ? null : type.typeInfo();
        return ti != null && type.arrays() == 0 && isInterface(ti) && !isFunctional(ti) && !isEntry(ti);
    }

    /** A class a {@code Box<dyn Any>} can be downcast to: no interface, no type parameters Any could not tell apart. */
    static boolean isDowncastTarget(ParameterizedType type) {
        TypeInfo ti = type == null ? null : type.typeInfo();
        return ti != null && type.arrays() == 0 && !type.isTypeParameter() && !ti.isJavaLangObject() && !ti.isInterface()
               && ti.typeParameters().isEmpty();
    }

    private static boolean isInterface(TypeInfo ti) {
        return ti.isInterface() && JDK.get(ti.fullyQualifiedName()) == null;
    }

    /**
     * A functional interface: a closure. A JDK one is, also when the front end has only loaded it lazily and has not
     * worked out its single abstract method (an in-memory parse does not complete the types it loads).
     */
    static boolean isFunctional(ParameterizedType type) {
        return type.isFunctionalInterface() || type.typeInfo() != null && type.arrays() == 0 && isFunctional(type.typeInfo());
    }

    /** A JDK functional interface printed as a {@code dyn Fn}, not an {@code FnMut}: see {@link #function}. */
    static boolean isFn(ParameterizedType type) {
        TypeInfo ti = type.typeInfo();
        if (ti == null || type.arrays() > 0 || !isFunctional(ti)) return false;
        return switch (ti.simpleName()) {
            case "Runnable", "Consumer", "BiConsumer" -> false;
            case "Supplier", "Function", "BiFunction", "Predicate", "UnaryOperator", "BinaryOperator" -> true;
            default -> {
                MethodInfo sam = ownAbstractMethod(ti);
                yield sam != null && !sam.noReturnValue();
            }
        };
    }

    /** The JDK's functional interfaces, as closure traits. */
    private static boolean isFunctional(TypeInfo ti) {
        return "java.util.function".equals(ti.packageName()) || "java.lang.Runnable".equals(ti.fullyQualifiedName());
    }

    /** {@code Function<T, R>} as {@code dyn Fn(&T) -> R}: the closure borrows what Java passes by reference. */
    private static String function(ParameterizedType type, RustContext context, Element where) {
        String name = type.typeInfo().simpleName();
        List<ParameterizedType> args = type.parameters();
        return switch (name) {
            case "Runnable" -> "dyn FnMut()";
            case "Supplier" -> "dyn Fn() -> " + arg(args, 0, context, where);
            case "Consumer" -> "dyn FnMut(" + borrowed(args, 0, context, where) + ")";
            case "BiConsumer" -> "dyn FnMut(" + borrowed(args, 0, context, where) + ", "
                                 + borrowed(args, 1, context, where) + ")";
            case "Function" -> "dyn Fn(" + borrowed(args, 0, context, where) + ") -> " + arg(args, 1, context, where);
            case "BiFunction" -> "dyn Fn(" + borrowed(args, 0, context, where) + ", " + borrowed(args, 1, context, where)
                                 + ") -> " + arg(args, 2, context, where);
            case "Predicate" -> "dyn Fn(" + borrowed(args, 0, context, where) + ") -> bool";
            case "UnaryOperator" -> "dyn Fn(" + borrowed(args, 0, context, where) + ") -> " + arg(args, 0, context, where);
            case "BinaryOperator" -> "dyn Fn(" + borrowed(args, 0, context, where) + ", " + borrowed(args, 0, context, where)
                                     + ") -> " + arg(args, 0, context, where);
            default -> {
                // the primitive specializations, BiPredicate, ToIntFunction, …: the shape of their own method
                List<String> parameters = samParameters(type, context, where);
                if (parameters == null) {
                    context.message(RustPrintCode.UNMAPPED_JDK, where, type.typeInfo().fullyQualifiedName());
                    yield "dyn Fn()";
                }
                MethodInfo sam = ownAbstractMethod(type.typeInfo());
                String result = sam.noReturnValue() ? "" : " -> " + samType(sam.returnType(), args, false, context, where);
                yield (sam.noReturnValue() ? "dyn FnMut(" : "dyn Fn(") + String.join(", ", parameters) + ")" + result;
            }
        };
    }

    /**
     * The parameter types of a JDK functional interface's own abstract method ({@code IntSupplier.getAsInt},
     * {@code ToIntFunction.applyAsInt}), its type parameters replaced by {@code type}'s arguments; {@code null} when
     * the method is inherited from a generic super-interface, whose type parameters are not {@code type}'s.
     */
    private static List<String> samParameters(ParameterizedType type, RustContext context, Element where) {
        MethodInfo sam = ownAbstractMethod(type.typeInfo());
        if (sam == null) return null;
        return sam.parameters().stream()
                .map(p -> samType(p.parameterizedType(), type.parameters(), true, context, where)).toList();
    }

    // the front end sets no single abstract method on a library type
    private static MethodInfo ownAbstractMethod(TypeInfo ti) {
        List<MethodInfo> methods = ti.methods().stream().filter(m -> m.isAbstract() && !m.isStatic()).toList();
        return methods.size() == 1 ? methods.getFirst() : null;
    }

    // a primitive as itself, a type parameter as the argument; a parameter borrowed as in 'borrowed'
    private static String samType(ParameterizedType t, List<ParameterizedType> args, boolean parameter,
                                  RustContext context, Element where) {
        if (t.isTypeParameter()) {
            int i = t.typeParameter().getIndex();
            return parameter ? borrowed(args, i, context, where) : arg(args, i, context, where);
        }
        if (t.isPrimitiveExcludingVoid() || !parameter || isCopy(t)) return owned(t, context, where);
        if (isString(t)) return "&str";
        return "&" + owned(t, context, where);
    }

    /** The parameter types of the closure a JDK functional interface stands for; {@code null} for another type. */
    static List<String> closureParameters(ParameterizedType type, RustContext context, Element where) {
        TypeInfo ti = type.typeInfo();
        if (ti == null || !isFunctional(ti)) return null;
        List<ParameterizedType> args = type.parameters();
        return switch (ti.simpleName()) {
            case "Runnable", "Supplier" -> List.of();
            case "Consumer", "Function", "Predicate", "UnaryOperator" -> List.of(borrowed(args, 0, context, where));
            case "BiConsumer", "BiFunction" -> List.of(borrowed(args, 0, context, where), borrowed(args, 1, context, where));
            case "BinaryOperator" -> List.of(borrowed(args, 0, context, where), borrowed(args, 0, context, where));
            default -> samParameters(type, context, where);
        };
    }

    private static String arg(List<ParameterizedType> args, int i, RustContext context, Element where) {
        return i < args.size() ? owned(args.get(i), context, where) : "()";
    }

    private static String borrowed(List<ParameterizedType> args, int i, RustContext context, Element where) {
        if (i >= args.size()) return "()";
        ParameterizedType a = args.get(i);
        if (isString(a)) return "&str"; // as a String parameter: a closure is called with one
        return isCopy(a) ? owned(a, context, where) : "&" + owned(a, context, where);
    }

    private static String named(ParameterizedType type, RustContext context, Element where) {
        TypeInfo ti = type.typeInfo();
        String base;
        Mapped mapped = JDK.get(ti.fullyQualifiedName());
        if (mapped != null) {
            if (mapped.use != null) context.use(mapped.use);
            base = mapped.rust;
        } else if (ti.compilationUnit().externalLibrary()) {
            if (ti.isJavaLangObject()) {
                context.use("std::any::Any");
                return "Box<dyn Any>";
            }
            if ("java.util.Iterator".equals(ti.fullyQualifiedName()) && type.parameters().size() == 1) {
                // Rust's Iterator names its element as an associated type
                return "Iterator<Item = " + owned(type.parameters().getFirst(), context, where) + ">";
            }
            context.message(RustPrintCode.UNMAPPED_JDK, where, ti.fullyQualifiedName());
            base = ti.simpleName();
        } else {
            base = RustNames.type(ti);
            String module = RustNames.module(ti.packageName());
            if (!module.equals(context.module)) context.use("crate::" + module + "::" + base);
            // a struct with borrowed fields: its lifetime, elided where a signature allows it; in its own impl, the
            // impl's, so that a static method returning one says what it borrows from
            if (context.borrows.hasLifetime(ti)) {
                String lifetime = ti == context.type ? RustBorrows.LIFETIME : "'_";
                return base + "<" + java.util.stream.Stream.concat(java.util.stream.Stream.of(lifetime),
                        type.parameters().stream().map(p -> owned(p, context, where))).collect(Collectors.joining(", ")) + ">";
            }
        }
        String named;
        if (type.parameters().isEmpty() || "String".equals(base)) {
            named = base;
        } else {
            named = base + "<" + type.parameters().stream().map(p -> owned(p, context, where))
                    .collect(Collectors.joining(", ")) + ">";
        }
        if (isShared(ti, context.facts)) {
            context.use("std::rc::Rc");
            return "Rc<" + named + ">";
        }
        return named;
    }

    static String primitive(TypeInfo ti) {
        if (ti.isInt()) return "i32";
        if (ti.isLong()) return "i64";
        if (ti.isShort()) return "i16";
        if (ti.isByte()) return "i8";
        if (ti.isDouble()) return "f64";
        if (ti.isFloat()) return "f32";
        if (ti.isBoolean()) return "bool";
        if (ti.isChar()) return "char";
        return "()";
    }

    /** The empty value of an owned type, for a field the constructor does not assign. */
    static String empty(ParameterizedType type, boolean nullable, RustContext context, Element where) {
        if (nullable) return "None";
        if (type.isPrimitiveExcludingVoid() && type.arrays() == 0) {
            TypeInfo ti = type.typeInfo();
            if (ti.isBoolean()) return "false";
            if (ti.isChar()) return "'\\0'";
            if (ti.isDouble() || ti.isFloat()) return "0.0";
            return "0";
        }
        String owned = owned(type, context, where);
        if (owned.equals("String")) return "String::new()";
        int lt = owned.indexOf('<');
        String base = lt < 0 ? owned : owned.substring(0, lt);
        return switch (base) {
            case "Vec", "HashMap", "HashSet", "BTreeMap", "BTreeSet", "VecDeque" -> base + "::new()";
            case "Option" -> "None";
            default -> {
                context.message(RustPrintCode.FIELD_DEFAULT, where, owned);
                yield "Default::default()";
            }
        };
    }
}
