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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Negation;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.print.translate.CstQueries;

/**
 * Java's constant expressions where C#'s compiler judges them otherwise: Java wraps {@code Integer.MAX_VALUE + 1} and
 * narrows {@code (byte) 200} silently, C# rejects both as a constant's overflow (CS0220, CS0221) unless
 * {@code unchecked}; Java saturates {@code (int) 3.9e10} and makes {@code (long) Double.NaN} zero, where C#'s
 * constant conversion is unspecified. The value of a constant expression, as Java computes it.
 */
final class CSharpConstants {

    private CSharpConstants() {
    }

    /**
     * The value of {@code e} when it is a constant expression of a primitive type: a literal, a negation, a cast, a
     * binary operation of constants, a static final field of the JDK or of the program with a constant initializer;
     * null otherwise. A char is its {@code Integer} code.
     */
    static Number value(Expression e) {
        Expression x = CstQueries.unwrap(e);
        return switch (x) {
            case ConstantExpression<?> c -> switch (c.constant()) {
                case null -> null;
                case Character ch -> (int) ch;
                case Number n -> n;
                default -> null;
            };
            case Negation n when n.expression().isNumeric() -> negate(value(n.expression()));
            case Cast c -> {
                Number v = value(c.expression());
                yield v == null ? null : convert(v, kind(c.parameterizedType()));
            }
            case BinaryOperator bo -> binary(bo);
            case VariableExpression ve when ve.variable() instanceof FieldReference fr -> field(fr.fieldInfo());
            default -> null;
        };
    }

    /** The primitive kind of a type: 'I' int, 'J' long, 'S' short, 'B' byte, 'C' char, 'F' float, 'D' double; 0 else. */
    static char kind(ParameterizedType t) {
        if (t == null || t.arrays() != 0 || t.typeInfo() == null) return 0;
        TypeInfo ti = t.typeInfo();
        if (ti.isInt()) return 'I';
        if (ti.isLong()) return 'J';
        if (ti.isShort()) return 'S';
        if (ti.isByte()) return 'B';
        if (ti.isChar()) return 'C';
        if (ti.isFloat()) return 'F';
        if (ti.isDouble()) return 'D';
        return 0;
    }

    /** A binary operation of integers whose exact result does not fit its type: C# rejects it as a constant. */
    static boolean overflows(BinaryOperator bo) {
        char kind = kind(bo.parameterizedType());
        if (kind != 'I' && kind != 'J') return false;
        Number l = value(bo.lhs());
        Number r = value(bo.rhs());
        if (l == null || r == null || l instanceof Double || l instanceof Float || r instanceof Double
            || r instanceof Float) {
            return false;
        }
        BigInteger a = BigInteger.valueOf(l.longValue());
        BigInteger b = BigInteger.valueOf(r.longValue());
        BigInteger exact = switch (bo.operator().name()) {
            case "+" -> a.add(b);
            case "-" -> a.subtract(b);
            case "*" -> a.multiply(b);
            default -> null;
        };
        if (exact == null) return false;
        long min = kind == 'I' ? Integer.MIN_VALUE : Long.MIN_VALUE;
        long max = kind == 'I' ? Integer.MAX_VALUE : Long.MAX_VALUE;
        return exact.compareTo(BigInteger.valueOf(min)) < 0 || exact.compareTo(BigInteger.valueOf(max)) > 0;
    }

    /** A cast of a constant to an integral type whose value does not fit it, which Java narrows and C# rejects. */
    static boolean narrowingOverflow(Cast cast) {
        char to = kind(cast.parameterizedType());
        Number v = value(cast.expression());
        if (v == null || to == 0 || to == 'F' || to == 'D') return false;
        if (v instanceof Double || v instanceof Float) return true;
        long l = v.longValue();
        return switch (to) {
            case 'B' -> l < Byte.MIN_VALUE || l > Byte.MAX_VALUE;
            case 'S' -> l < Short.MIN_VALUE || l > Short.MAX_VALUE;
            case 'C' -> l < 0 || l > Character.MAX_VALUE;
            case 'I' -> l < Integer.MIN_VALUE || l > Integer.MAX_VALUE;
            default -> false;
        };
    }

    /** The literal of Java's value of a constant floating-point value cast to an integral type: saturated, NaN 0. */
    static String floatingToIntegral(Cast cast) {
        char to = kind(cast.parameterizedType());
        Number v = value(cast.expression());
        if (!(v instanceof Double || v instanceof Float) || to == 0 || to == 'F' || to == 'D') return null;
        Number result = convert(v, to);
        return switch (to) {
            case 'J' -> result.longValue() + "L";
            case 'C' -> "(char) " + result.intValue();
            case 'B' -> "(sbyte) " + parenthesizedNegative(result.intValue());
            case 'S' -> "(short) " + parenthesizedNegative(result.intValue());
            default -> Integer.toString(result.intValue()).equals("-2147483648") ? "int.MinValue"
                    : Integer.toString(result.intValue());
        };
    }

    private static String parenthesizedNegative(int i) {
        return i < 0 ? "(" + i + ")" : Integer.toString(i);
    }

    private static Number negate(Number v) {
        return switch (v) {
            case null -> null;
            case Integer i -> -i;
            case Long l -> -l;
            case Float f -> -f;
            case Double d -> -d;
            default -> -v.intValue();
        };
    }

    /** Java's conversion of a primitive value to the kind. */
    private static Number convert(Number v, char to) {
        boolean floating = v instanceof Double || v instanceof Float;
        return switch (to) {
            case 'I' -> floating ? (int) v.doubleValue() : (int) v.longValue();
            case 'J' -> floating ? (long) v.doubleValue() : v.longValue();
            case 'S' -> floating ? (short) (int) v.doubleValue() : (short) v.longValue();
            case 'B' -> floating ? (byte) (int) v.doubleValue() : (byte) v.longValue();
            case 'C' -> floating ? (int) (char) (int) v.doubleValue() : (int) (char) v.longValue();
            case 'F' -> v.floatValue();
            case 'D' -> v.doubleValue();
            default -> null;
        };
    }

    /** Java's value of a binary operation of constants, wrapped as Java wraps it; null when not one. */
    private static Number binary(BinaryOperator bo) {
        char kind = kind(bo.parameterizedType());
        if (kind == 0) return null;
        Number l = value(bo.lhs());
        Number r = value(bo.rhs());
        if (l == null || r == null) return null;
        String op = bo.operator().name();
        try {
            return switch (kind) {
                case 'I' -> {
                    int a = l.intValue();
                    int b = r.intValue();
                    yield switch (op) {
                        case "+" -> a + b;
                        case "-" -> a - b;
                        case "*" -> a * b;
                        case "/" -> a / b;
                        case "%" -> a % b;
                        case "<<" -> a << b;
                        case ">>" -> a >> b;
                        case ">>>" -> a >>> b;
                        default -> null;
                    };
                }
                case 'J' -> {
                    long a = l.longValue();
                    long b = r.longValue();
                    yield switch (op) {
                        case "+" -> a + b;
                        case "-" -> a - b;
                        case "*" -> a * b;
                        case "/" -> a / b;
                        case "%" -> a % b;
                        case "<<" -> a << b;
                        case ">>" -> a >> b;
                        case ">>>" -> a >>> b;
                        default -> null;
                    };
                }
                case 'D', 'F' -> {
                    double a = l.doubleValue();
                    double b = r.doubleValue();
                    Double d = switch (op) {
                        case "+" -> a + b;
                        case "-" -> a - b;
                        case "*" -> a * b;
                        case "/" -> a / b;
                        default -> null;
                    };
                    yield d == null ? null : kind == 'F' ? (Number) d.floatValue() : d;
                }
                default -> null;
            };
        } catch (ArithmeticException e) {
            return null; // a constant division by zero is no constant
        }
    }

    /** A static final field's value: the JDK's by reflection, the program's from its constant initializer. */
    private static Number field(FieldInfo f) {
        if (!f.isStatic() || !f.isFinal()) return null;
        TypeInfo owner = f.typeInfo();
        if (CSharpNames.translated(owner)) {
            return f.initializer() == null || f.initializer().isEmpty() ? null : value(f.initializer());
        }
        String fqn = owner.fullyQualifiedName();
        if (!fqn.startsWith("java.lang.")) return null;
        try {
            Field field = Class.forName(fqn).getField(f.name());
            if (!Modifier.isStatic(field.getModifiers()) || !field.getType().isPrimitive()) return null;
            Object v = field.get(null);
            return v instanceof Character ch ? (Number) (int) ch : v instanceof Number n ? n : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
