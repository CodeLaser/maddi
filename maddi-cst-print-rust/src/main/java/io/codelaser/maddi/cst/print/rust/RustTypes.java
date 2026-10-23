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
            Map.entry("java.util.Optional", new Mapped("Option", null)));

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
        TypeInfo ti = type.typeInfo();
        return type.arrays() == 0 && ti != null && !ti.compilationUnit().externalLibrary() && !ti.isInterface()
               && !ti.typeNature().isEnum() && facts.immutable(ti);
    }

    /** Copied rather than moved or borrowed: a primitive, a boxed primitive, a plain enum. */
    static boolean isCopy(ParameterizedType type) {
        if (type.arrays() > 0 || type.typeInfo() == null) return false;
        TypeInfo ti = type.typeInfo();
        if (ti.isPrimitiveExcludingVoid() || ti.isBoxedExcludingVoid()) return true;
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
        if (form != Form.OWNED && ti != null && type.arrays() == 0 && isInterface(ti) && !isFunctional(ti)) {
            return (form == Form.SHARED ? "&dyn " : "&mut dyn ") + named(type, context, where);
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
        if (type.isTypeParameter()) return type.typeParameter().simpleName();
        TypeInfo ti = type.typeInfo();
        if (ti == null) {
            // an unbounded wildcard, '?'
            context.use("std::any::Any");
            return "Box<dyn Any>";
        }
        if (isFunctional(ti)) return "Box<" + function(type, context, where) + ">";
        if (isInterface(ti)) return "Box<dyn " + named(type, context, where) + ">";
        return named(type, context, where);
    }

    private static boolean isInterface(TypeInfo ti) {
        return ti.isInterface() && JDK.get(ti.fullyQualifiedName()) == null;
    }

    /** The JDK's functional interfaces, as closure traits. */
    private static boolean isFunctional(TypeInfo ti) {
        return ti.packageName().equals("java.util.function") || "java.lang.Runnable".equals(ti.fullyQualifiedName());
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
            default -> {
                context.message(RustPrintCode.UNMAPPED_JDK, where, type.typeInfo().fullyQualifiedName());
                yield "dyn Fn()";
            }
        };
    }

    private static String arg(List<ParameterizedType> args, int i, RustContext context, Element where) {
        return i < args.size() ? owned(args.get(i), context, where) : "()";
    }

    private static String borrowed(List<ParameterizedType> args, int i, RustContext context, Element where) {
        if (i >= args.size()) return "()";
        ParameterizedType a = args.get(i);
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
            context.message(RustPrintCode.UNMAPPED_JDK, where, ti.fullyQualifiedName());
            base = ti.simpleName();
        } else {
            base = RustNames.type(ti);
            String module = RustNames.module(ti.packageName());
            if (!module.equals(context.module)) context.use("crate::" + module + "::" + base);
        }
        if (type.parameters().isEmpty()) return base;
        if ("String".equals(base)) return base;
        return base + "<" + type.parameters().stream().map(p -> owned(p, context, where))
                .collect(Collectors.joining(", ")) + ">";
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
