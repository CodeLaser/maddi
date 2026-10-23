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
            "append", "insert", "sort", "retainAll");

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
        String o = printer.text(mc.object());
        String collection = RustTypes.mapped(owner);
        if ("String".equals(collection)) return string(o, name, args, printer, mc);
        if ("java.lang.Object".equals(fqn)) {
            return switch (name) {
                case "equals" -> value(o + " == " + printer.reference(args.getFirst()));
                case "toString" -> value("format!(\"{:?}\", " + o + ")");
                case "hashCode" -> null;
                default -> null;
            };
        }
        if (collection == null) return null;
        return switch (collection) {
            case "Vec" -> vec(o, name, args, printer, mc);
            case "HashMap", "BTreeMap" -> map(o, name, args, printer, mc);
            case "HashSet", "BTreeSet" -> set(o, name, args, printer);
            case "VecDeque" -> deque(o, name, args, printer);
            case "Option" -> option(o, name, args, printer);
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
            case "java.lang.Long.parseLong/1" -> value(printer.text(args.getFirst()) + ".parse::<i64>().unwrap()");
            case "java.lang.String.valueOf/1", "java.lang.Integer.toString/1", "java.lang.Long.toString/1" ->
                    value(operand(printer, args.getFirst()) + ".to_string()");
            case "java.util.List.of/0", "java.util.Collections.emptyList/0" -> value("Vec::new()");
            case "java.util.Optional.empty/0" -> new Printed("None", Held.VALUE, true);
            case "java.util.Optional.of/1", "java.util.Optional.ofNullable/1" ->
                    value("Some(" + printer.ownedText(args.getFirst()) + ")");
            default -> {
                if (fqn.equals("java.util.List") && name.equals("of")) {
                    yield value("vec![" + String.join(", ", args.stream().map(printer::ownedText).toList()) + "]");
                }
                yield null;
            }
        };
    }

    /** {@code System.out.println(x)}: a call on the field {@code System.out}. */
    private static Printed printStream(boolean err, MethodCall mc, RustBodyPrinter printer) {
        String name = mc.methodInfo().name();
        String macro = ("println".equals(name) ? (err ? "eprintln" : "println") : (err ? "eprint" : "print")) + "!";
        if (mc.parameterExpressions().isEmpty()) return value(macro + "()");
        return value(macro + "(\"{}\", " + printer.text(mc.parameterExpressions().getFirst()) + ")");
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
            case "equals/1" -> value(o + " == " + printer.text(args.getFirst()));
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
            case "append/1" -> value(o + ".push_str(&" + printer.text(args.getFirst()) + ".to_string())");
            default -> null;
        };
    }

    private static Printed vec(String o, String name, List<Expression> args, RustBodyPrinter printer, MethodCall mc) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "add/1" -> value(o + ".push(" + printer.ownedText(args.getFirst()) + ")");
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
            case "getLast/0" -> RustBodyPrinter.printed(o + "[" + o + ".len() - 1]", Held.PLACE, false);
            default -> null;
        };
    }

    private static Printed map(String o, String name, List<Expression> args, RustBodyPrinter printer, MethodCall mc) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "put/2" -> value(o + ".insert(" + printer.ownedText(args.get(0)) + ", " + printer.ownedText(args.get(1)) + ")");
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
            case "values/0" -> value(o + ".values()");
            default -> null;
        };
    }

    private static Printed set(String o, String name, List<Expression> args, RustBodyPrinter printer) {
        int n = args.size();
        return switch (name + "/" + n) {
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

    private static Printed option(String o, String name, List<Expression> args, RustBodyPrinter printer) {
        int n = args.size();
        return switch (name + "/" + n) {
            case "isPresent/0" -> value(o + ".is_some()");
            case "isEmpty/0" -> value(o + ".is_none()");
            case "get/0", "orElseThrow/0" -> value(o + ".unwrap()");
            case "orElse/1" -> value(o + ".unwrap_or(" + printer.ownedText(args.getFirst()) + ")");
            default -> null;
        };
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
            case "java.lang.Math.E" -> "std::f64::consts::E";
            case "java.lang.Integer.MAX_VALUE" -> "i32::MAX";
            case "java.lang.Integer.MIN_VALUE" -> "i32::MIN";
            case "java.lang.Long.MAX_VALUE" -> "i64::MAX";
            case "java.lang.Long.MIN_VALUE" -> "i64::MIN";
            case "java.lang.Double.MAX_VALUE" -> "f64::MAX";
            default -> null;
        };
    }

    private static boolean stringElements(MethodCall mc) {
        List<io.codelaser.maddi.cst.api.type.ParameterizedType> parameters = mc.object().parameterizedType().parameters();
        return !parameters.isEmpty() && RustTypes.isString(parameters.getFirst());
    }

    static Printed construct(ConstructorCall cc, RustBodyPrinter printer) {
        TypeInfo ti = cc.parameterizedType().typeInfo();
        String mapped = RustTypes.mapped(ti);
        if (mapped == null) return null;
        List<Expression> args = cc.parameterExpressions();
        if ("String".equals(mapped)) {
            return value(args.isEmpty() ? "String::new()" : printer.text(args.getFirst()) + ".to_string()");
        }
        if (args.isEmpty() || args.getFirst().parameterizedType().isPrimitiveExcludingVoid()) {
            // new ArrayList<>(), new ArrayList<>(capacity)
            RustTypes.owned(cc.parameterizedType(), printer.context(), cc); // registers the 'use'
            return value(mapped + "::new()");
        }
        // a copy constructor: new ArrayList<>(other); from another kind of collection, or a map's keys or values,
        // the elements are collected
        Expression source = args.getFirst();
        String text = printer.text(source);
        TypeInfo sourceType = source.parameterizedType().typeInfo();
        boolean sameKind = sourceType != null && mapped.equals(RustTypes.mapped(sourceType));
        if (sameKind) return value(text + ".clone()");
        boolean iterator = text.endsWith(".keys()") || text.endsWith(".values()");
        return value(text + (iterator ? "" : ".iter()") + ".cloned().collect()");
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
