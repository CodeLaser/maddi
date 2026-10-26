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

import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.print.rust.RustBodyPrinter.Held;
import io.codelaser.maddi.cst.print.rust.RustBodyPrinter.Printed;

import java.util.List;
import java.util.Set;

/**
 * The JDK's methods as {@code std}'s, for the types {@link RustTypes} maps: {@code String}, the collections,
 * {@code Optional}, and a few of {@code System}, {@code Math}, {@code Objects}, {@code Integer}. An index is an
 * {@code i32} in Java and a {@code usize} in Rust; a size goes back to {@code i32}, so that the arithmetic around it
 * stays Java's. {@code null} means "not in the mapping": the caller reports it.
 */
final class RustJdk {

    /** The methods that modify their receiver, by simple name, for the collections and {@code StringBuilder}. */
    private static final Set<String> MODIFYING = Set.of("add", "addAll", "put", "putAll", "remove", "removeAll",
            "clear", "set", "push", "pop", "poll", "offer", "addFirst", "addLast", "removeFirst", "removeLast",
            "append", "insert", "sort", "retainAll", "merge", "compute", "computeIfAbsent", "computeIfPresent",
            "putIfAbsent", "replaceAll", "removeIf");

    private RustJdk() {
    }

    static boolean modifies(MethodCall mc) {
        return MODIFYING.contains(mc.methodInfo().name());
    }

    static Printed call(MethodCall mc, RustBodyPrinter printer) {
        MethodInfo m = mc.methodInfo();
        TypeInfo owner = m.typeInfo();
        String fqn = owner.fullyQualifiedName();
        List<Expression> args = mc.parameterExpressions();
        String name = m.name();
        if (m.isStatic()) return staticCall(fqn, name, args, printer, mc);
        if (mc.object() == null) return null;
        if ("java.io.PrintStream".equals(fqn) && ("println".equals(name) || "print".equals(name))
            && mc.object() instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
            && "java.lang.System".equals(fr.fieldInfo().owner().fullyQualifiedName())) {
            return printStream("err".equals(fr.fieldInfo().name()), mc, printer);
        }
        // what the caller does with this call's result modifies it ('m.get(k).touch()'): the receiver is reached
        // exclusively, the arguments are only read
        boolean mutating = printer.mutatingPlace(false);
        // a nullable receiver is unwrapped, Java's NPE as a panic; an Optional is Rust's Option itself
        String o = "Option".equals(RustTypes.mapped(owner)) || fqn.startsWith("java.util.stream.")
                ? printer.text(mc.object()) : printer.receiver(mc.object(), modifies(mc) || mutating);
        if ("java.lang.Enum".equals(fqn)) {
            return switch (name + "/" + args.size()) {
                case "ordinal/0" -> value(o + ".ordinal()");
                case "name/0", "toString/0" -> RustBodyPrinter.printed(o + ".name()", Held.STR, false);
                default -> null;
            };
        }
        if (fqn.startsWith("java.util.stream.")) return stream(o, fqn, name, args, printer);
        if (RustTypes.isEntry(owner)) return entry(o, name, mc);
        if ("java.math.BigDecimal".equals(fqn)) return bigDecimal(paren(o), name, args, printer, mc);
        if (fqn.startsWith("java.util.function.") || "java.lang.Runnable".equals(fqn)) {
            // a functional interface is a Box<dyn Fn…>: calling its method calls it. Its closure borrows what Java
            // passes by reference (RustTypes.function), so a value or a place is borrowed: f.apply(list.get(0))
            return value("(" + o + ")(" + String.join(", ", args.stream().map(a -> RustTypes.isCopy(a.parameterizedType())
                    ? printer.text(a) : RustBodyPrinter.adapt(printer.printed(a), Held.SHARED)).toList()) + ")");
        }
        if ("SystemTime".equals(RustTypes.mapped(owner))) {
            return switch (name + "/" + args.size()) {
                case "isBefore/1" -> value(o + " < " + printer.text(args.getFirst()));
                case "isAfter/1" -> value(o + " > " + printer.text(args.getFirst()));
                case "equals/1" -> value(o + " == " + printer.text(args.getFirst()));
                case "toString/0" -> value("format!(\"{:?}\", " + o + ")");
                default -> null;
            };
        }
        if ("java.lang.Boolean".equals(fqn) && "equals".equals(name)) {
            Expression a = args.getFirst();
            if (isObject(a.parameterizedType())) {
                // Boolean.TRUE.equals(map.get("active")): a Box<dyn Any> holding a bool
                Printed p = printer.printed(a);
                String any = p.nullable() ? p.text() + ".and_then(|v| v.downcast_ref::<bool>())"
                        : p.text() + ".downcast_ref::<bool>()";
                return value("(" + any + " == Some(&" + o + "))");
            }
            return value(o + " == " + printer.text(a));
        }
        String kind = RustTypes.mapped(owner);
        if ("stream/0".equals(name + "/" + args.size()) && kind != null && Set.of("Vec", "HashSet", "BTreeSet", "VecDeque")
                .contains(kind)) {
            // values the stream may hand on as copies: a primitive, a string, an @Immutable class
            List<io.codelaser.maddi.cst.api.type.ParameterizedType> ps = mc.object().parameterizedType().parameters();
            io.codelaser.maddi.cst.api.type.ParameterizedType element = ps.isEmpty() ? null : ps.getFirst();
            if (element != null && RustTypes.isCopy(element)) return value(iter(o) + ".copied()");
            if (element != null && (RustTypes.isString(element)
                                    || RustTypes.isImmutableValue(element, printer.context().facts))) {
                return value(iter(o) + ".cloned()");
            }
            return value(iter(o));
        }
        String collection = RustTypes.mapped(owner);
        if ("String".equals(collection)) return string(o, name, args, printer, mc);
        if ("java.lang.Object".equals(fqn)) {
            return switch (name) {
                case "equals" -> value(o + " == " + printer.reference(args.getFirst()));
                case "toString" -> isObject(mc.object().parameterizedType())
                        ? value("crate::jrt::to_string(" + printer.reference(mc.object()) + ")")
                        : value("format!(\"{:?}\", " + o + ")");
                case "hashCode" -> null;
                default -> null;
            };
        }
        if (collection == null) return null;
        if ("io.codelaser.maddi.support.SetOnce".equals(fqn)) return setOnce(o, name, args, printer);
        return switch (collection) {
            case "Vec" -> vec(o, name, args, printer, mc);
            case "HashMap", "BTreeMap" -> mutating && "get".equals(name) && args.size() == 1
                    ? RustBodyPrinter.printed(o + ".get_mut(" + printer.reference(args.getFirst()) + ")", Held.EXCLUSIVE, true)
                    : map(o, name, args, printer, mc);
            case "HashSet", "BTreeSet" -> set(o, name, args, printer);
            case "VecDeque" -> deque(o, name, args, printer);
            case "Option" -> option(o, name, args, printer, printer.printed(mc.object()).held());
            default -> null;
        };
    }

    private static Printed staticCall(String fqn, String name, List<Expression> args, RustBodyPrinter printer,
                                      MethodCall mc) {
        int n = args.size();
        return switch (fqn + "." + name + "/" + n) {
            case "java.lang.Math.max/2" -> value(operand(printer, args.get(0)) + ".max(" + printer.text(args.get(1)) + ")");
            case "java.lang.Math.min/2" -> value(operand(printer, args.get(0)) + ".min(" + printer.text(args.get(1)) + ")");
            case "java.lang.Math.abs/1" -> value(operand(printer, args.getFirst()) + ".abs()");
            case "java.lang.Math.sqrt/1" -> value(operand(printer, args.getFirst()) + ".sqrt()");
            case "java.util.Objects.equals/2" -> value(printer.text(args.get(0)) + " == " + printer.text(args.get(1)));
            case "java.util.Objects.requireNonNull/1", "java.util.Objects.requireNonNull/2" ->
                    same(printer.text(args.getFirst()));
            case "java.lang.Integer.parseInt/1" -> value(printer.text(args.getFirst()) + ".parse::<i32>().unwrap()");
            case "java.lang.Integer.hashCode/1", "java.lang.Long.hashCode/1" -> value(printer.text(args.getFirst()));
            case "java.lang.Character.toLowerCase/1" -> value(operand(printer, args.getFirst()) + ".to_ascii_lowercase()");
            case "java.lang.Character.toUpperCase/1" -> value(operand(printer, args.getFirst()) + ".to_ascii_uppercase()");
            case "java.lang.Character.isDigit/1" -> value(operand(printer, args.getFirst()) + ".is_ascii_digit()");
            case "java.lang.Character.isLetter/1" -> value(operand(printer, args.getFirst()) + ".is_alphabetic()");
            case "java.lang.Character.isLetterOrDigit/1" -> value(operand(printer, args.getFirst()) + ".is_alphanumeric()");
            case "java.lang.Character.isWhitespace/1" -> value(operand(printer, args.getFirst()) + ".is_whitespace()");
            case "java.lang.Character.isUpperCase/1" -> value(operand(printer, args.getFirst()) + ".is_uppercase()");
            case "java.lang.Character.isLowerCase/1" -> value(operand(printer, args.getFirst()) + ".is_lowercase()");
            case "java.lang.System.currentTimeMillis/0" -> value("(std::time::SystemTime::now().duration_since("
                                                                 + "std::time::UNIX_EPOCH).unwrap().as_millis() as i64)");
            case "java.time.LocalDateTime.now/0", "java.time.Instant.now/0" -> {
                printer.context().message(RustPrintCode.TIME_AS_SYSTEM_TIME, mc, null);
                printer.context().use("std::time::SystemTime");
                yield value("SystemTime::now()");
            }
            case "java.lang.System.nanoTime/0" -> value("(std::time::SystemTime::now().duration_since("
                                                        + "std::time::UNIX_EPOCH).unwrap().as_nanos() as i64)");
            case "java.math.BigDecimal.valueOf/1" -> {
                printer.context().message(RustPrintCode.BIGDECIMAL_AS_F64, mc, null);
                yield value("(" + printer.text(args.getFirst()) + " as f64)");
            }
            case "java.util.Collections.unmodifiableList/1", "java.util.Collections.unmodifiableSet/1",
                 "java.util.Collections.unmodifiableMap/1", "java.util.Collections.unmodifiableCollection/1",
                 "java.util.Collections.unmodifiableSortedSet/1", "java.util.Collections.unmodifiableSortedMap/1" ->
                    // a read-only view: Rust's shared borrow is exactly that
                    printer.printed(args.getFirst());
            case "java.util.Collections.emptySet/0", "java.util.Set.of/0" -> {
                printer.context().use("std::collections::HashSet");
                yield value("HashSet::new()");
            }
            case "java.util.Collections.emptyMap/0", "java.util.Map.of/0" -> {
                printer.context().use("std::collections::HashMap");
                yield value("HashMap::new()");
            }
            case "java.util.Collections.singletonList/1" -> value("vec![" + printer.ownedText(args.getFirst()) + "]");
            case "java.util.List.copyOf/1", "java.util.Set.copyOf/1", "java.util.Map.copyOf/1" ->
                    value(printer.text(args.getFirst()) + (printer.movable(args.getFirst()) ? "" : ".clone()"));
            case "java.util.stream.IntStream.range/2" ->
                    value("(" + printer.text(args.get(0)) + ".." + printer.text(args.get(1)) + ")");
            case "java.util.stream.IntStream.rangeClosed/2" ->
                    value("(" + printer.text(args.get(0)) + "..=" + printer.text(args.get(1)) + ")");
            case "java.lang.Long.parseLong/1" -> value(printer.text(args.getFirst()) + ".parse::<i64>().unwrap()");
            case "java.lang.String.valueOf/1", "java.lang.Integer.toString/1", "java.lang.Long.toString/1" ->
                    isObject(args.getFirst().parameterizedType())
                            ? value("crate::jrt::to_string(" + printer.reference(args.getFirst()) + ")")
                            : value(operand(printer, args.getFirst()) + ".to_string()");
            case "java.util.Collections.sort/2" -> {
                String c = comparator(args.get(1), printer);
                yield c == null ? null : value(printer.receiver(args.getFirst(), true) + ".sort_by(" + c + ")");
            }
            case "java.util.List.of/0", "java.util.Collections.emptyList/0" -> value("Vec::new()");
            case "java.util.Map.entry/2" ->
                    value("(" + printer.ownedText(args.get(0)) + ", " + printer.ownedText(args.get(1)) + ")");
            case "java.lang.System.exit/1" -> value("std::process::exit(" + printer.text(args.getFirst()) + ")");
            case "java.lang.System.lineSeparator/0" -> value("String::from(\"\\n\")");
            case "java.lang.System.arraycopy/5" -> value(arraycopy(args, printer));
            case "java.util.Collections.sort/1" -> value(printer.receiver(args.getFirst(), true) + ".sort()");
            case "java.util.Collections.singleton/1" -> {
                printer.context().use("std::collections::HashSet");
                yield value("HashSet::from([" + printer.ownedText(args.getFirst()) + "])");
            }
            case "java.util.Collections.synchronizedList/1", "java.util.Collections.synchronizedSet/1",
                 "java.util.Collections.synchronizedMap/1", "java.util.Collections.synchronizedCollection/1" -> {
                // the lock goes: exclusive access is the borrow checker's
                printer.context().message(RustPrintCode.SYNCHRONIZED, mc, null);
                yield printer.printed(args.getFirst());
            }
            case "java.util.Arrays.fill/2" ->
                    value(printer.receiver(args.get(0), true) + ".fill(" + printer.ownedText(args.get(1)) + ")");
            case "java.util.Arrays.toString/1" -> value("format!(\"{:?}\", " + printer.text(args.getFirst()) + ")");
            case "java.util.Arrays.copyOf/2" -> value("{ let mut c = " + printer.text(args.get(0))
                    + ".to_vec(); c.resize(" + printer.text(args.get(1)) + " as usize, Default::default()); c }");
            case "java.util.Optional.empty/0" -> new Printed("None", Held.VALUE, true);
            case "java.util.Optional.of/1", "java.util.Optional.ofNullable/1" ->
                    value("Some(" + printer.ownedText(args.getFirst()) + ")");
            default -> {
                if (fqn.equals("java.util.Collections") && name.equals("addAll") && n >= 2) {
                    List<String> values = args.subList(1, n).stream().map(printer::ownedText).toList();
                    String into = printer.receiver(args.getFirst(), true);
                    yield n == 2 && args.get(1).parameterizedType().arrays() > 0
                            ? value(into + ".extend(" + printer.text(args.get(1)) + ".iter().cloned())")
                            : value(into + ".extend([" + String.join(", ", values) + "])");
                }
                if (fqn.equals("java.util.Objects") && name.equals("hash")) yield hash(args, printer);
                if (fqn.equals("java.lang.String") && name.equals("format") && n >= 1) yield format(args, printer, mc);
                if (fqn.equals("java.lang.String") && name.equals("join") && n >= 2) yield join(args, printer);
                if ((fqn.equals("java.util.Arrays") && name.equals("asList"))
                    || (fqn.equals("java.util.stream.Stream") && name.equals("of"))) {
                    String vec = "vec![" + String.join(", ", args.stream().map(printer::ownedText).toList()) + "]";
                    yield value(fqn.endsWith("Stream") ? vec + ".into_iter()" : vec);
                }
                if (fqn.equals("java.util.Set") && name.equals("of")) {
                    printer.context().use("std::collections::HashSet");
                    yield value("HashSet::from([" + String.join(", ", args.stream().map(printer::ownedText).toList()) + "])");
                }
                if (fqn.equals("java.util.Map") && name.equals("of") && n % 2 == 0) {
                    printer.context().use("std::collections::HashMap");
                    List<String> pairs = new java.util.ArrayList<>();
                    for (int i = 0; i < n; i += 2) {
                        pairs.add("(" + printer.ownedText(args.get(i)) + ", " + printer.ownedText(args.get(i + 1)) + ")");
                    }
                    yield value("HashMap::from([" + String.join(", ", pairs) + "])");
                }
                if (fqn.equals("java.util.List") && name.equals("of")) {
                    yield value("vec![" + String.join(", ", args.stream().map(printer::ownedText).toList()) + "]");
                }
                yield null;
            }
        };
    }

    /**
     * {@code Objects.hash(a, b)}: each value hashed into std's default hasher. The numbers differ from Java's, which
     * only matters to code that depends on them.
     */
    private static Printed hash(List<Expression> args, RustBodyPrinter printer) {
        StringBuilder sb = new StringBuilder("{ let mut h = std::hash::DefaultHasher::new(); ");
        for (Expression a : args) sb.append("std::hash::Hash::hash(").append(printer.reference(a)).append(", &mut h); ");
        return value(sb.append("std::hash::Hasher::finish(&h) as i32 }").toString());
    }

    /** {@code System.out.println(x)}: a call on the field {@code System.out}. */
    private static Printed printStream(boolean err, MethodCall mc, RustBodyPrinter printer) {
        String name = mc.methodInfo().name();
        String macro = ("println".equals(name) ? (err ? "eprintln" : "println") : (err ? "eprint" : "print")) + "!";
        if (mc.parameterExpressions().isEmpty()) return value(macro + "()");
        String[] d = printer.display(mc.parameterExpressions().getFirst());
        return value(macro + "(\"" + d[0] + "\", " + d[1] + ")");
    }

    private static Printed string(String o, String name, List<Expression> args, RustBodyPrinter printer,
                                  MethodCall mc) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "length/0" -> {
                printer.context().message(RustPrintCode.STRING_UNITS, mc, "length");
                yield value(o + ".len() as i32");
            }
            case "isEmpty/0" -> value(o + ".is_empty()");
            case "equals/1" -> stringEquals(o, args.getFirst(), printer);
            case "contains/1" -> value(o + ".contains(" + printer.reference(args.getFirst()) + ")");
            case "startsWith/1" -> value(o + ".starts_with(" + printer.reference(args.getFirst()) + ")");
            case "endsWith/1" -> value(o + ".ends_with(" + printer.reference(args.getFirst()) + ")");
            case "trim/0", "strip/0" -> value(o + ".trim().to_string()");
            case "toUpperCase/0" -> value(o + ".to_uppercase()");
            case "toLowerCase/0" -> value(o + ".to_lowercase()");
            case "toString/0" -> value(o + ".to_string()");
            case "charAt/1" -> {
                printer.context().message(RustPrintCode.STRING_UNITS, mc, "charAt");
                yield value(o + ".chars().nth(" + printer.index(args.getFirst()) + ").unwrap()");
            }
            case "substring/1" -> value(o + "[" + printer.index(args.getFirst()) + "..].to_string()");
            case "substring/2" -> value(o + "[" + printer.index(args.get(0)) + ".." + printer.index(args.get(1))
                                        + "].to_string()");
            case "append/1" -> value(append(o, args.getFirst(), printer));
            case "equalsIgnoreCase/1" -> {
                // false for a null argument, as in Java
                Printed a = printer.printed(args.getFirst());
                if (!a.nullable()) yield value(o + ".eq_ignore_ascii_case(" + printer.reference(args.getFirst()) + ")");
                yield value(a.text() + ".as_ref().map_or(false, |v| " + o + ".eq_ignore_ascii_case(v))");
            }
            case "isBlank/0" -> value(o + ".trim().is_empty()");
            case "getBytes/0", "getBytes/1" -> value(o + ".as_bytes().iter().map(|b| *b as i8).collect::<Vec<i8>>()");
            case "hashCode/0" -> value("crate::jrt::string_hash_code(" + o + ")");
            case "indexOf/2" -> {
                String from = printer.index(args.get(1));
                yield value(o + "[" + from + "..].find(" + pattern(args.getFirst(), printer) + ").map(|i| (i + "
                            + from + ") as i32).unwrap_or(-1)");
            }
            case "repeat/1" -> value(o + ".repeat(" + printer.index(args.getFirst()) + ")");
            case "indexOf/1" -> value(o + ".find(" + pattern(args.getFirst(), printer) + ").map(|i| i as i32).unwrap_or(-1)");
            case "lastIndexOf/1" ->
                    value(o + ".rfind(" + pattern(args.getFirst(), printer) + ").map(|i| i as i32).unwrap_or(-1)");
            case "replace/2" -> value(o + ".replace(" + pattern(args.get(0), printer) + ", "
                                      + replacement(args.get(1), printer) + ")");
            case "split/1" -> value(o + ".split(" + pattern(args.getFirst(), printer)
                                    + ").map(|s| s.to_string()).collect::<Vec<_>>()");
            case "compareTo/1" -> value("(" + o + ".cmp(" + printer.reference(args.getFirst()) + ") as i32)");
            case "concat/1" -> value("format!(\"{}{}\", " + o + ", " + printer.text(args.getFirst()) + ")");
            default -> null;
        };
    }

    /**
     * {@code s.equals(x)}: Java's false for a null or a non-string {@code x}; an {@code Object} is downcast first.
     */
    private static Printed stringEquals(String o, Expression arg, RustBodyPrinter printer) {
        Printed a = printer.printed(arg);
        io.codelaser.maddi.cst.api.type.ParameterizedType type = arg.parameterizedType();
        boolean object = type.typeInfo() != null && type.typeInfo().isJavaLangObject();
        String self = o.startsWith("\"") ? o : "&" + paren(o) + "[..]";
        String test = object ? "v.downcast_ref::<String>().map_or(false, |s| s.as_str() == " + self + ")"
                : "v.as_str() == " + self;
        if (a.nullable()) return value(a.text() + ".as_ref().map_or(false, |v| " + test + ")");
        if (object) return value("{ let v = &" + a.text() + "; " + test + " }");
        return value(o + " == " + a.text());
    }

    /** {@code sb.append(x)}: a char is pushed, a string pushed as a {@code &str}, anything else formatted first. */
    static String append(String target, Expression arg, RustBodyPrinter printer) {
        io.codelaser.maddi.cst.api.type.ParameterizedType type = arg.parameterizedType();
        if (type.isPrimitiveExcludingVoid() && type.typeInfo().isChar()) return target + ".push(" + printer.text(arg) + ")";
        if (RustTypes.isString(type) && !printer.printed(arg).nullable()) {
            return target + ".push_str(" + printer.reference(arg) + ")";
        }
        if (type.isPrimitiveExcludingVoid()) return target + ".push_str(&" + operand(printer, arg) + ".to_string())";
        String[] d = printer.display(arg);
        return target + ".push_str(&format!(\"" + d[0] + "\", " + d[1] + "))";
    }

    /**
     * A stream as an iterator. Java's {@code distinct} keeps the first of equal elements, in order; Rust's iterators
     * have no such adapter, so the elements are folded into a vector.
     */
    private static Printed stream(String o, String fqn, String name, List<Expression> args, RustBodyPrinter printer) {
        String sum = switch (fqn) {
            case "java.util.stream.LongStream" -> "i64";
            case "java.util.stream.DoubleStream" -> "f64";
            default -> "i32";
        };
        return switch (name + "/" + args.size()) {
            case "map/1", "mapToInt/1", "mapToLong/1", "mapToDouble/1", "mapToObj/1" ->
                    value(o + ".map(" + printer.text(args.getFirst()) + ")");
            case "filter/1" -> value(o + ".filter(" + printer.text(args.getFirst()) + ")");
            case "anyMatch/1" -> value(o + ".any(" + printer.text(args.getFirst()) + ")");
            case "allMatch/1" -> value(o + ".all(" + printer.text(args.getFirst()) + ")");
            case "noneMatch/1" -> value("!" + o + ".any(" + printer.text(args.getFirst()) + ")");
            case "forEach/1" -> value(o + ".for_each(" + printer.text(args.getFirst()) + ")");
            case "distinct/0" ->
                    value(o + ".fold(Vec::new(), |mut v, x| { if !v.contains(&x) { v.push(x); } v }).into_iter()");
            case "toList/0" -> value(o + ".collect::<Vec<_>>()");
            case "reduce/2" -> value(o + ".fold(" + printer.text(args.get(0)) + ", " + printer.text(args.get(1)) + ")");
            case "reduce/1" -> new Printed(o + ".reduce(" + printer.text(args.getFirst()) + ")", Held.VALUE, true);
            case "limit/1" -> value(o + ".take(" + printer.index(args.getFirst()) + ")");
            case "skip/1" -> value(o + ".skip(" + printer.index(args.getFirst()) + ")");
            case "flatMap/1" -> value(o + ".flat_map(" + printer.text(args.getFirst()) + ")");
            case "findFirst/0", "findAny/0" -> new Printed(o + ".next()", Held.VALUE, true);
            case "boxed/0", "parallel/0", "sequential/0" -> value(o);
            case "sorted/0" -> value("{ let mut v = " + o + ".collect::<Vec<_>>(); v.sort(); v }.into_iter()");
            case "sorted/1" -> {
                String c = comparator(args.getFirst(), printer);
                yield c == null ? null
                        : value("{ let mut v = " + o + ".collect::<Vec<_>>(); v.sort_by(" + c + "); v }.into_iter()");
            }
            case "max/1", "min/1" -> {
                String c = comparator(args.getFirst(), printer);
                yield c == null ? null : new Printed(o + "." + name + "_by(" + c + ")", Held.VALUE, true);
            }
            case "average/0" -> new Printed("{ let v = " + o + ".map(|x| x as f64).collect::<Vec<f64>>(); "
                                            + "if v.is_empty() { None } else { Some(v.iter().sum::<f64>() / v.len() as f64) } }",
                    Held.VALUE, true);
            case "count/0" -> value(o + ".count() as i64");
            case "sum/0" -> value(o + ".sum::<" + sum + ">()");
            case "collect/1" -> collect(o, args.getFirst(), printer);
            default -> null;
        };
    }

    /** The collectors with an obvious counterpart: to a list, to a set, joining strings. */
    private static Printed collect(String o, Expression collector, RustBodyPrinter printer) {
        if (!(collector instanceof MethodCall c)
            || !"java.util.stream.Collectors".equals(c.methodInfo().typeInfo().fullyQualifiedName())) {
            return null;
        }
        return switch (c.methodInfo().name() + "/" + c.parameterExpressions().size()) {
            case "toList/0", "toUnmodifiableList/0" -> value(o + ".collect::<Vec<_>>()");
            case "toSet/0", "toUnmodifiableSet/0" -> {
                printer.context().use("std::collections::HashSet");
                yield value(o + ".collect::<HashSet<_>>()");
            }
            case "joining/0" -> value(o + ".collect::<Vec<_>>().join(\"\")");
            case "joining/1" -> value(o + ".collect::<Vec<_>>().join(" + printer.text(c.parameterExpressions().getFirst()) + ")");
            default -> null;
        };
    }

    /** {@code entry.getKey()}: the entries of a map's iterator are {@code (&K, &V)} tuples. */
    private static Printed entry(String o, String name, MethodCall mc) {
        List<io.codelaser.maddi.cst.api.type.ParameterizedType> kv = mc.object().parameterizedType().parameters();
        int index = switch (name) {
            case "getKey" -> 0;
            case "getValue" -> 1;
            default -> -1;
        };
        if (index < 0) return null;
        boolean copy = kv.size() == 2 && RustTypes.isCopy(kv.get(index));
        String part = paren(o) + "." + index;
        // a map's iteration yields (&K, &V), an entry held elsewhere (K, V): clone() copies the part either way
        return copy ? value(part + ".clone()") : RustBodyPrinter.printed(part, Held.SHARED, false);
    }

    /** {@code map.getOrDefault(k, d)}: a copy for a copied value; otherwise a borrow, of the default too. */
    /**
     * {@code map.getOrDefault(k, new X()).put(...)}: the value modified in place, or the default, which nothing keeps.
     * {@code null} when {@code e} is not such a call.
     */
    static String getOrDefaultMut(Expression e, RustBodyPrinter printer) {
        if (!(e instanceof MethodCall mc) || mc.object() == null || mc.parameterExpressions().size() != 2
            || !"getOrDefault".equals(mc.methodInfo().name())
            || !Set.of("HashMap", "BTreeMap").contains(String.valueOf(RustTypes.mapped(mc.methodInfo().typeInfo())))) {
            return null;
        }
        List<Expression> args = mc.parameterExpressions();
        return printer.receiver(mc.object(), true) + ".get_mut(" + printer.reference(args.get(0)) + ").unwrap_or(&mut "
               + printer.ownedText(args.get(1)) + ")";
    }

    private static Printed getOrDefault(String o, List<Expression> args, RustBodyPrinter printer, MethodCall mc) {
        String get = o + ".get(" + printer.reference(args.get(0)) + ")";
        if (copyValues(mc)) return value(get + ".copied().unwrap_or(" + operand(printer, args.get(1)) + ")");
        List<io.codelaser.maddi.cst.api.type.ParameterizedType> kv = mc.object().parameterizedType().parameters();
        if (kv.size() == 2 && RustTypes.isString(kv.get(1))) {
            return value(get + ".cloned().unwrap_or_else(|| " + printer.ownedText(args.get(1)) + ")");
        }
        return RustBodyPrinter.printed(get + ".unwrap_or(" + printer.reference(args.get(1)) + ")", Held.SHARED, false);
    }

    /** {@code BigDecimal} as {@code f64}: the arithmetic is the operators, the scale a rounding. */
    private static Printed bigDecimal(String o, String name, List<Expression> args, RustBodyPrinter printer,
                                      MethodCall mc) {
        printer.context().message(RustPrintCode.BIGDECIMAL_AS_F64, mc, name);
        String x = args.isEmpty() ? null : operand(printer, args.getFirst());
        return switch (name + "/" + args.size()) {
            case "add/1", "add/2" -> value("(" + o + " + " + x + ")");
            case "subtract/1", "subtract/2" -> value("(" + o + " - " + x + ")");
            case "multiply/1", "multiply/2" -> value("(" + o + " * " + x + ")");
            case "divide/1", "divide/2", "divide/3" -> value("(" + o + " / " + x + ")");
            case "remainder/1" -> value("(" + o + " % " + x + ")");
            case "negate/0" -> value("(-" + o + ")");
            case "abs/0" -> value(o + ".abs()");
            case "max/1" -> value(o + ".max(" + x + ")");
            case "min/1" -> value(o + ".min(" + x + ")");
            case "compareTo/1" -> value("(" + o + ".partial_cmp(&" + x + ").unwrap() as i32)");
            case "equals/1" -> value(o + " == " + x);
            case "signum/0" -> value("(if " + o + " > 0.0 { 1 } else if " + o + " < 0.0 { -1 } else { 0 })");
            case "setScale/1", "setScale/2" -> value("((" + o + " * 10f64.powi(" + x + ")).round() / 10f64.powi(" + x + "))");
            case "doubleValue/0" -> value(o);
            case "intValue/0" -> value("(" + o + " as i32)");
            case "longValue/0" -> value("(" + o + " as i64)");
            case "toString/0", "toPlainString/0" -> value(o + ".to_string()");
            default -> null;
        };
    }

    /**
     * {@code String.format} with a constant format: {@code %s %d} are {@code {}}, {@code %.2f} is {@code {:.2}},
     * {@code %n} a newline. Another specifier is reported, and printed as {@code {}}.
     */
    private static Printed format(List<Expression> args, RustBodyPrinter printer, MethodCall mc) {
        if (!(args.getFirst() instanceof io.codelaser.maddi.cst.api.expression.StringConstant sc)) return null;
        String f = sc.constant();
        StringBuilder out = new StringBuilder();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("%([-#+ 0,(]*)(\\d+)?(\\.\\d+)?([a-zA-Z%])").matcher(f);
        int at = 0;
        int conversions = 0;
        List<String> shown = new java.util.ArrayList<>();
        while (m.find()) {
            out.append(braces(f.substring(at, m.start())));
            at = m.end();
            String flags = m.group(1), width = m.group(2), precision = m.group(3), conversion = m.group(4);
            switch (conversion) {
                case "%" -> out.append('%');
                case "n" -> out.append('\n');
                case "s", "d", "f", "S", "x", "X", "c", "b", "e" -> {
                    int argIndex = ++conversions;
                    String spec = (flags.contains("-") ? "<" : "") + (flags.contains("0") ? "0" : "")
                                  + (width == null ? "" : width) + (precision == null ? "" : precision)
                                  + switch (conversion) {
                                        case "x" -> "x";
                                        case "X" -> "X";
                                        case "e" -> "e";
                                        default -> "";
                                    };
                    String[] d = argIndex < args.size() ? printer.display(args.get(argIndex)) : new String[]{"{}", ""};
                    if (argIndex < args.size()) shown.add(d[1]);
                    if (d[0].equals("{}")) out.append(spec.isEmpty() ? "{}" : "{:" + spec + "}");
                    else out.append(d[0]);
                }
                default -> {
                    printer.context().message(RustPrintCode.FORMAT, mc, m.group());
                    out.append("{}");
                }
            }
        }
        out.append(braces(f.substring(at)));
        String text = RustBodyPrinter.stringLiteral(out.toString());
        List<String> rest = shown.size() == args.size() - 1 ? shown
                : args.subList(1, args.size()).stream().map(printer::text).toList();
        return value("format!(" + text + (rest.isEmpty() ? "" : ", " + String.join(", ", rest)) + ")");
    }

    private static String braces(String literal) {
        return literal.replace("{", "{{").replace("}", "}}");
    }

    /** {@code String.join(sep, list)}, or {@code String.join(sep, a, b, …)}. */
    private static Printed join(List<Expression> args, RustBodyPrinter printer) {
        String sep = printer.text(args.getFirst());
        if (args.size() == 2 && !RustTypes.isString(args.get(1).parameterizedType())) {
            String parts = printer.text(args.get(1));
            if (isIterator(parts)) return value(parts + ".cloned().collect::<Vec<_>>().join(" + sep + ")");
            return value(parts + ".join(" + sep + ")");
        }
        String parts = String.join(", ", args.subList(1, args.size()).stream()
                .map(a -> printer.text(a) + ".to_string()").toList());
        return value("vec![" + parts + "].join(" + sep + ")");
    }

    /** What {@code str::find}, {@code split} and {@code replace} take: a char or a {@code &str}. */
    private static String pattern(Expression e, RustBodyPrinter printer) {
        if (e.parameterizedType().isPrimitiveExcludingVoid()) return printer.text(e);
        return printer.reference(e);
    }

    private static String replacement(Expression e, RustBodyPrinter printer) {
        if (e.parameterizedType().isPrimitiveExcludingVoid()) return "&" + printer.text(e) + ".to_string()";
        return printer.reference(e);
    }

    /** A collection's iterator; a map's {@code keys()}, {@code values()} and {@code iter()} are iterators already. */
    static String iter(String o) {
        return isIterator(o) ? o : o + ".iter()";
    }

    static boolean isIterator(String o) {
        return o.endsWith(".keys()") || o.endsWith(".values()") || o.endsWith(".iter()");
    }

    private static String paren(String text) {
        return text.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_' || c == '.') ? text : "(" + text + ")";
    }

    /** A JDK method reference's body, the parameters named: {@code String::length} is {@code x.len() as i32}. */
    static String reference(MethodInfo m, List<String> params) {
        String key = m.typeInfo().fullyQualifiedName() + "." + m.name() + "/" + params.size();
        return switch (key) {
            case "java.lang.String.length/1" -> params.getFirst() + ".len() as i32";
            case "java.lang.String.isEmpty/1" -> params.getFirst() + ".is_empty()";
            case "java.lang.String.toUpperCase/1" -> params.getFirst() + ".to_uppercase()";
            case "java.lang.String.toLowerCase/1" -> params.getFirst() + ".to_lowercase()";
            case "java.lang.String.trim/1", "java.lang.String.strip/1" -> params.getFirst() + ".trim().to_string()";
            case "java.lang.String.valueOf/1", "java.lang.Object.toString/1", "java.lang.Integer.toString/1" ->
                    params.getFirst() + ".to_string()";
            case "java.lang.Integer.parseInt/1" -> params.getFirst() + ".parse::<i32>().unwrap()";
            case "java.lang.Integer.sum/2", "java.lang.Long.sum/2", "java.lang.Double.sum/2",
                 "java.math.BigDecimal.add/2" -> params.get(0) + " + " + params.get(1);
            case "java.lang.Integer.max/2", "java.lang.Math.max/2" -> params.get(0) + ".max(" + params.get(1) + ")";
            case "java.lang.Integer.min/2", "java.lang.Math.min/2" -> params.get(0) + ".min(" + params.get(1) + ")";
            case "java.lang.Double.doubleValue/1", "java.lang.Integer.intValue/1" -> params.getFirst();
            case "java.util.Map.Entry.getKey/1" -> params.getFirst() + ".0";
            case "java.util.Map.Entry.getValue/1" -> params.getFirst() + ".1";
            case "java.util.Collection.stream/1", "java.util.List.stream/1", "java.util.Set.stream/1" ->
                    params.getFirst() + ".iter()";
            case "java.util.Objects.nonNull/1" -> params.getFirst() + ".is_some()";
            case "java.util.Objects.isNull/1" -> params.getFirst() + ".is_none()";
            default -> null;
        };
    }

    private static Printed vec(String o, String name, List<Expression> args, RustBodyPrinter printer, MethodCall mc) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "add/1" -> value(o + ".push(" + printer.ownedAs(args.getFirst(), typeArgument(mc, 0)) + ")");
            case "sort/1" -> {
                String c = comparator(args.getFirst(), printer);
                yield c == null ? null : value(o + ".sort_by(" + c + ")");
            }
            case "add/2" -> value(o + ".insert(" + printer.index(args.get(0)) + ", " + printer.ownedText(args.get(1)) + ")");
            case "get/1" -> RustBodyPrinter.printed(o + "[" + printer.index(args.getFirst()) + "]", Held.PLACE, false);
            case "set/2" -> value(o + "[" + printer.index(args.get(0)) + "] = " + printer.ownedText(args.get(1)));
            case "size/0" -> value(o + ".len() as i32");
            case "isEmpty/0" -> value(o + ".is_empty()");
            case "clear/0" -> value(o + ".clear()");
            case "contains/1" -> stringElements(mc)
                    // Vec<String>::contains takes a &String; a &str compares with each element instead
                    ? value(o + ".iter().any(|e| e == " + printer.reference(args.getFirst()) + ")")
                    : value(o + ".contains(" + printer.reference(args.getFirst()) + ")");
            case "addAll/1" -> value(o + ".extend(" + printer.text(args.getFirst()) + ".iter().cloned())");
            case "getFirst/0" -> RustBodyPrinter.printed(o + "[0]", Held.PLACE, false);
            case "forEach/1" -> value(iter(o) + ".for_each(" + printer.text(args.getFirst()) + ")");
            case "removeIf/1" -> value(o + ".retain(|x| !" + printer.applied(args.getFirst(), List.of("x")) + ")");
            case "indexOf/1" -> value(o + ".iter().position(|e| e == " + printer.reference(args.getFirst())
                                      + ").map(|i| i as i32).unwrap_or(-1)");
            case "remove/1" -> args.getFirst().parameterizedType().isInt()
                    ? value(o + ".remove(" + printer.index(args.getFirst()) + ")")
                    : value("if let Some(p) = " + o + ".iter().position(|e| e == " + printer.reference(args.getFirst())
                            + ") { " + o + ".remove(p); }");
            case "getLast/0" -> RustBodyPrinter.printed(o + "[" + o + ".len() - 1]", Held.PLACE, false);
            default -> null;
        };
    }

    private static Printed map(String o, String name, List<Expression> args, RustBodyPrinter printer, MethodCall mc) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "putAll/1" -> value(o + ".extend(" + printer.text(args.getFirst())
                                     + ".iter().map(|(k, v)| (k.clone(), v.clone())))");
            case "put/2" -> {
                String key = printer.ownedAs(args.get(0), typeArgument(mc, 0));
                String v = printer.ownedAs(args.get(1), typeArgument(mc, 1));
                // the value reads the map being modified (m.put(k, m.get(k) + 1)): computed before the exclusive borrow
                String root = o.replaceFirst("[.\\[(].*$", "");
                if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(root) + "\\b").matcher(v).find()) {
                    yield value("{ let v = " + v + "; " + o + ".insert(" + key + ", v) }");
                }
                yield value(o + ".insert(" + key + ", " + v + ")");
            }
            // Java's null for an absent key is Rust's None
            case "get/1" -> copyValues(mc)
                    ? RustBodyPrinter.printed(o + ".get(" + printer.reference(args.getFirst()) + ").copied()", Held.VALUE, true)
                    : RustBodyPrinter.printed(o + ".get(" + printer.reference(args.getFirst()) + ")", Held.SHARED, true);
            case "containsKey/1" -> value(o + ".contains_key(" + printer.reference(args.getFirst()) + ")");
            case "remove/1" -> new Printed(o + ".remove(" + printer.reference(args.getFirst()) + ")", Held.VALUE, true);
            case "size/0" -> value(o + ".len() as i32");
            case "isEmpty/0" -> value(o + ".is_empty()");
            case "clear/0" -> value(o + ".clear()");
            case "keySet/0" -> value(o + ".keys()");
            case "entrySet/0" -> value(o + ".iter()");
            case "getOrDefault/2" -> getOrDefault(o, args, printer, mc);
            case "computeIfAbsent/2" -> RustBodyPrinter.printed(o + ".entry(" + printer.ownedText(args.get(0))
                    + ").or_insert_with_key(" + printer.text(args.get(1)) + ")", Held.EXCLUSIVE, false);
            case "putIfAbsent/2" -> value("{ " + o + ".entry(" + printer.ownedText(args.get(0)) + ").or_insert("
                                          + printer.ownedText(args.get(1)) + "); }");
            case "merge/3" -> copyValues(mc)
                    ? value("{ let v = " + printer.text(args.get(1)) + "; " + o + ".entry(" + printer.ownedText(args.get(0))
                            + ").and_modify(|e| *e = (" + printer.text(args.get(2)) + ")(*e, v)).or_insert(v); }")
                    : null;
            case "forEach/1" -> value(o + ".iter().for_each(" + printer.tupleLambda(args.getFirst()) + ")");
            case "values/0" -> value(o + ".values()");
            default -> null;
        };
    }

    private static Printed set(String o, String name, List<Expression> args, RustBodyPrinter printer) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "forEach/1" -> value(o + ".iter().for_each(" + printer.text(args.getFirst()) + ")");
            case "addAll/1" -> value(o + ".extend(" + printer.text(args.getFirst()) + ".iter().cloned())");
            case "add/1" -> value(o + ".insert(" + printer.ownedText(args.getFirst()) + ")");
            case "contains/1" -> value(o + ".contains(" + printer.reference(args.getFirst()) + ")");
            case "remove/1" -> value(o + ".remove(" + printer.reference(args.getFirst()) + ")");
            case "size/0" -> value(o + ".len() as i32");
            case "isEmpty/0" -> value(o + ".is_empty()");
            case "clear/0" -> value(o + ".clear()");
            default -> null;
        };
    }

    private static Printed deque(String o, String name, List<Expression> args, RustBodyPrinter printer) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "add/1", "addLast/1", "offer/1", "offerLast/1" -> value(o + ".push_back(" + printer.ownedText(args.getFirst()) + ")");
            case "addFirst/1", "push/1", "offerFirst/1" -> value(o + ".push_front(" + printer.ownedText(args.getFirst()) + ")");
            case "poll/0", "pollFirst/0", "pop/0", "removeFirst/0" -> new Printed(o + ".pop_front()", Held.VALUE, true);
            case "pollLast/0", "removeLast/0" -> new Printed(o + ".pop_back()", Held.VALUE, true);
            case "peek/0", "peekFirst/0" -> new Printed(o + ".front()", Held.SHARED, true);
            case "size/0" -> value(o + ".len() as i32");
            case "isEmpty/0" -> value(o + ".is_empty()");
            default -> null;
        };
    }

    /** maddi's {@code SetOnce}: an {@code Option}, set by its {@code @Mark}, read by its {@code @Only(after)}. */
    private static Printed setOnce(String o, String name, List<Expression> args, RustBodyPrinter printer) {
        return switch (name + "/" + args.size()) {
            case "set/1" -> value(o + " = Some(" + printer.ownedText(args.getFirst()) + ")");
            case "get/0", "get/1" -> RustBodyPrinter.printed(o + ".as_ref().unwrap()", Held.SHARED, false);
            case "isSet/0" -> value(o + ".is_some()");
            default -> null;
        };
    }

    private static Printed option(String o, String name, List<Expression> args, RustBodyPrinter printer, Held held) {
        int n = args.size();
        // a borrowed or stored Option is not taken: its closures see a reference to the value
        String r = held == Held.VALUE ? o : o + ".as_ref()";
        String owned = held == Held.VALUE ? o : o + ".clone()";
        return switch (name + "/" + n) {
            case "isPresent/0" -> value(o + ".is_some()");
            case "isEmpty/0" -> value(o + ".is_none()");
            case "get/0", "orElseThrow/0", "getAsInt/0", "getAsLong/0", "getAsDouble/0" -> value(o + ".unwrap()");
            // the content or the default, owned: a borrowed or stored Option is copied rather than moved out of
            case "orElse/1" -> value(owned + ".unwrap_or(" + printer.copiedText(args.getFirst()) + ")");
            case "orElseGet/1" -> value(owned + ".unwrap_or_else(" + printer.text(args.getFirst()) + ")");
            case "orElseThrow/1" -> value(o + ".unwrap()");
            case "map/1" -> {
                Printed f = printer.optionalMapper(args.getFirst());
                yield new Printed(r + (f.nullable() ? ".and_then(" : ".map(") + f.text() + ")", Held.VALUE, true);
            }
            case "filter/1" -> new Printed(r + ".filter(" + printer.text(args.getFirst()) + ")", Held.VALUE, true);
            case "ifPresent/1" -> value(o + ".iter().for_each(" + printer.text(args.getFirst()) + ")");
            default -> null;
        };
    }

    private static io.codelaser.maddi.cst.api.type.ParameterizedType typeArgument(MethodCall mc, int i) {
        List<io.codelaser.maddi.cst.api.type.ParameterizedType> ps = mc.object().parameterizedType().parameters();
        return i < ps.size() ? ps.get(i) : null;
    }

    /** A map whose values are copied out: {@code get} gives an {@code Option<V>}, not an {@code Option<&V>}. */
    private static boolean copyValues(MethodCall mc) {
        List<io.codelaser.maddi.cst.api.type.ParameterizedType> parameters = mc.object().parameterizedType().parameters();
        return parameters.size() == 2 && RustTypes.isCopy(parameters.get(1));
    }

    /** The JDK's constants: {@code Math.PI}, {@code Integer.MAX_VALUE}, … {@code null} for another field. */
    static String constant(io.codelaser.maddi.cst.api.info.FieldInfo f) {
        return switch (f.owner().fullyQualifiedName() + "." + f.name()) {
            case "java.lang.Math.PI" -> "std::f64::consts::PI";
            case "java.lang.Boolean.TRUE" -> "true";
            case "java.lang.Boolean.FALSE" -> "false";
            case "java.lang.Math.E" -> "std::f64::consts::E";
            case "java.lang.Integer.MAX_VALUE" -> "i32::MAX";
            case "java.lang.Integer.MIN_VALUE" -> "i32::MIN";
            case "java.lang.Long.MAX_VALUE" -> "i64::MAX";
            case "java.lang.Long.MIN_VALUE" -> "i64::MIN";
            case "java.lang.Double.MAX_VALUE" -> "f64::MAX";
            case "java.math.BigDecimal.ZERO" -> "0.0_f64";
            case "java.math.BigDecimal.ONE" -> "1.0_f64";
            case "java.math.BigDecimal.TEN" -> "10.0_f64";
            default -> null;
        };
    }

    private static boolean isObject(io.codelaser.maddi.cst.api.type.ParameterizedType type) {
        return type.typeInfo() != null && type.typeInfo().isJavaLangObject() && type.arrays() == 0;
    }

    /**
     * A {@code Comparator} as the closure Rust's {@code sort_by}, {@code max_by} take: {@code |a, b| -> Ordering}.
     * {@code null} when it is not one of the shapes {@link #compare} knows.
     */
    static String comparator(Expression e, RustBodyPrinter printer) {
        String body = compare(e, printer, "a", "b");
        return body == null ? null : "|a, b| " + body;
    }

    /**
     * The {@code Ordering} of {@code a} and {@code b} under the comparator {@code e}. {@code partial_cmp} rather than
     * {@code cmp}, so that a double key compares. The key extractors are applied inline: a closure literal called
     * on the spot leaves rustc without the type of its parameter.
     */
    private static String compare(Expression e, RustBodyPrinter printer, String a, String b) {
        Expression c = io.codelaser.maddi.cst.print.translate.CstQueries.unwrap(e);
        if (c instanceof io.codelaser.maddi.cst.api.expression.Lambda l && l.methodInfo().parameters().size() == 2) {
            return printer.applied(l, List.of(a, b)) + ".cmp(&0)";
        }
        if (!(c instanceof MethodCall mc)) return null;
        String fqn = mc.methodInfo().typeInfo().fullyQualifiedName();
        String name = mc.methodInfo().name();
        List<Expression> args = mc.parameterExpressions();
        if (mc.methodInfo().isStatic()) {
            return switch (fqn + "." + name + "/" + args.size()) {
                case "java.util.Comparator.comparing/1", "java.util.Comparator.comparingInt/1",
                     "java.util.Comparator.comparingLong/1", "java.util.Comparator.comparingDouble/1" ->
                        byKey(args.getFirst(), printer, a, b);
                case "java.util.Map.Entry.comparingByValue/0" -> a + ".1.partial_cmp(&" + b + ".1).unwrap()";
                case "java.util.Map.Entry.comparingByKey/0" -> a + ".0.partial_cmp(&" + b + ".0).unwrap()";
                case "java.util.Comparator.naturalOrder/0" -> a + ".partial_cmp(" + b + ").unwrap()";
                case "java.util.Comparator.reverseOrder/0", "java.util.Collections.reverseOrder/0" ->
                        b + ".partial_cmp(" + a + ").unwrap()";
                default -> null;
            };
        }
        if (!"java.util.Comparator".equals(fqn) || mc.object() == null) return null;
        return switch (name + "/" + args.size()) {
            case "reversed/0" -> compare(mc.object(), printer, b, a);
            case "thenComparing/1", "thenComparingInt/1", "thenComparingLong/1", "thenComparingDouble/1" -> {
                String first = compare(mc.object(), printer, a, b);
                if (first == null) yield null;
                String next = compare(args.getFirst(), printer, a, b);
                if (next == null) next = byKey(args.getFirst(), printer, a, b);
                yield first + ".then_with(|| " + next + ")";
            }
            default -> null;
        };
    }

    private static String byKey(Expression key, RustBodyPrinter printer, String a, String b) {
        return printer.applied(key, List.of(a)) + ".partial_cmp(&" + printer.applied(key, List.of(b)) + ").unwrap()";
    }

    private static boolean stringElements(MethodCall mc) {
        List<io.codelaser.maddi.cst.api.type.ParameterizedType> parameters = mc.object().parameterizedType().parameters();
        return !parameters.isEmpty() && RustTypes.isString(parameters.getFirst());
    }

    /**
     * {@code System.arraycopy(src, sp, dst, dp, n)}: the slice copied over, element by element as Java does;
     * within one array, {@code copy_within}, which allows the ranges to overlap.
     */
    private static String arraycopy(List<Expression> args, RustBodyPrinter printer) {
        String src = printer.text(args.get(0));
        String sp = "(" + printer.text(args.get(1)) + ") as usize";
        String dst = printer.receiver(args.get(2), true);
        String dp = "(" + printer.text(args.get(3)) + ") as usize";
        String n = "(" + printer.text(args.get(4)) + ") as usize";
        if (src.equals(dst)) return dst + ".copy_within(" + sp + ".." + sp + " + " + n + ", " + dp + ")";
        return dst + "[" + dp + ".." + dp + " + " + n + "].clone_from_slice(&" + src + "[" + sp + ".." + sp + " + " + n
               + "])";
    }

    static Printed construct(ConstructorCall cc, RustBodyPrinter printer) {
        TypeInfo ti = cc.parameterizedType().typeInfo();
        if (ti != null && RustTypes.isEntry(ti) && cc.parameterExpressions().size() == 2) {
            List<Expression> kv = cc.parameterExpressions();
            return value("(" + printer.ownedText(kv.get(0)) + ", " + printer.ownedText(kv.get(1)) + ")");
        }
        String mapped = RustTypes.mapped(ti);
        if (mapped == null) return null;
        List<Expression> args = cc.parameterExpressions();
        if ("String".equals(mapped)) {
            return value(args.isEmpty() ? "String::new()" : printer.text(args.getFirst()) + ".to_string()");
        }
        if ("java.math.BigDecimal".equals(ti.fullyQualifiedName()) && args.size() == 1) {
            printer.context().message(RustPrintCode.BIGDECIMAL_AS_F64, cc, null);
            Expression a = args.getFirst();
            if (RustTypes.isString(a.parameterizedType())) return value(printer.text(a) + ".parse::<f64>().unwrap()");
            return value("(" + printer.text(a) + " as f64)");
        }
        if ("Option".equals(mapped)) return value("None");
        if (args.isEmpty() || args.getFirst().parameterizedType().isPrimitiveExcludingVoid()) {
            // new ArrayList<>(), new ArrayList<>(capacity)
            RustTypes.owned(cc.parameterizedType(), printer.context(), cc); // registers the 'use'
            return value(mapped + "::new()");
        }
        // a copy constructor: new ArrayList<>(other); from another kind of collection, or a map's keys or values,
        // the elements are collected
        Expression source = args.getFirst();
        // a nullable source: Java's NPE as a panic
        String text = printer.receiver(source, false);
        TypeInfo sourceType = source.parameterizedType().typeInfo();
        boolean sameKind = sourceType != null && mapped.equals(RustTypes.mapped(sourceType));
        if (sameKind) return value(printer.movable(source) ? text : text + ".clone()");
        boolean iterator = text.endsWith(".keys()") || text.endsWith(".values()");
        // the collection named: a collect() whose result is used on the spot ('.len()') has no type to infer
        String into = RustTypes.owned(cc.parameterizedType(), printer.context(), cc);
        return value(text + (iterator ? "" : ".iter()") + ".cloned().collect::<" + into + ">()");
    }

    private static Printed value(String text) {
        return RustBodyPrinter.printed(text, Held.VALUE, false);
    }

    private static Printed same(String text) {
        return RustBodyPrinter.printed(text, Held.VALUE, false);
    }

    private static String operand(RustBodyPrinter printer, Expression e) {
        String text = printer.text(e);
        return text.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_' || c == '.') ? text : "(" + text + ")";
    }
}
